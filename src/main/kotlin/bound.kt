// B2 STRESS FIXTURE (kotlin): BOUND
// EFFECT: boundedness:UNBOUNDED -> boundedness:BOUNDED
// OBSERVABLE AT: R1 (syntax only -- no execution, no compilation, no dataflow required to see it)
// GROUNDED IN: no CodeQL kotlin support -- grounded in Kotlin stdlib / JVM interop, cross-referenced
//   against codeql_census/repo/java/ql/lib/semmle/code/java/JDK.qll (java.nio.ByteBuffer's
//   position/limit/remaining() surface, java.lang.StringBuilder, java.io.RandomAccessFile -- kotlin's
//   own IntArray/List/String indexing is the same JVM array/index access these Java classes wrap) and
//   codeql_census/repo/java/ql/lib/semmle/code/java/frameworks/Jdbc.qll (TypeConnection,
//   TypePreparedStatement) for the SQL LIMIT/OFFSET realization. Kotlin's own bounds-safe stdlib
//   accessors (getOrElse, getOrNull, coerceIn) have no JDK/CodeQL analogue -- those are grounded
//   purely in the Kotlin standard library itself.
// REALIZATIONS (noun x verb-spelling, each CLOSED + OPEN):
//   1. noun=int array element       verb=index in array.indices check
//   2. noun=byte buffer window      verb=ByteBuffer.remaining() check
//   3. noun=list element            verb=List.getOrElse() bounds-safe accessor
//   4. noun=string character        verb=index < str.length check
//   5. noun=byte array slice        verb=coerceIn() clamp before copyOfRange
//   6. noun=query page offset       verb=offset range check before LIMIT/OFFSET
//   7. noun=string builder position verb=index bound check before deleteCharAt()
//   8. noun=random-access file offset verb=range check before RandomAccessFile.seek()

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.sql.Connection

// --- realization 1: noun=int array element, verb=index in array.indices check -----
// CLOSED
fun boundArrayAccessClosed(values: IntArray, index: Int): Int {
    require(index in values.indices) { "index out of range" }   // bound check precedes...
    return values[index]                                          // ...the indexed access
}

// OPEN
fun boundArrayAccessOpen(values: IntArray, index: Int): Int {
    return values[index]                        // indexed access with no bound check anywhere before it
}

// --- realization 2: noun=byte buffer window, verb=ByteBuffer.remaining() check ----
// CLOSED
fun boundBufferReadClosed(buffer: ByteBuffer, out: ByteArray, length: Int) {
    require(buffer.remaining() >= length) { "not enough bytes remaining" }   // bound check precedes...
    buffer.get(out, 0, length)                                                // ...the indexed access
}

// OPEN
fun boundBufferReadOpen(buffer: ByteBuffer, out: ByteArray, length: Int) {
    buffer.get(out, 0, length)                  // read with no remaining()/capacity check anywhere before it
}

// --- realization 3: noun=list element, verb=List.getOrElse() bounds-safe accessor -
// CLOSED
fun boundListAccessClosed(items: List<String>, index: Int): String {
    return items.getOrElse(index) { "" }        // bounds-safe accessor -- the check is built into the call
}

// OPEN
fun boundListAccessOpen(items: List<String>, index: Int): String {
    return items[index]                         // raw indexed access with no bound check anywhere before it
}

// --- realization 4: noun=string character, verb=index < str.length check ----------
// CLOSED
fun boundStringAccessClosed(text: String, index: Int): Char? {
    if (index !in text.indices) return null     // bound check precedes...
    return text[index]                           // ...the indexed access
}

// OPEN
fun boundStringAccessOpen(text: String, index: Int): Char {
    return text[index]                          // indexed access with no bound check anywhere before it
}

// --- realization 5: noun=byte array slice, verb=coerceIn() clamp before copyOfRange -
// CLOSED
fun boundArraySliceClosed(data: ByteArray, from: Int, to: Int): ByteArray {
    val start = from.coerceIn(0, data.size)     // bound check precedes...
    val end = to.coerceIn(start, data.size)
    return data.copyOfRange(start, end)          // ...the indexed access
}

// OPEN
fun boundArraySliceOpen(data: ByteArray, from: Int, to: Int): ByteArray {
    return data.copyOfRange(from, to)           // sliced with no clamp/bound check anywhere before it
}

// --- realization 6: noun=query page offset, verb=offset range check before LIMIT/OFFSET -
// CLOSED
fun boundPageOffsetClosed(connection: Connection, offset: Int, total: Int) {
    require(offset in 0 until total) { "offset out of range" }   // bound check precedes...
    val statement = connection.prepareStatement("SELECT * FROM items LIMIT 20 OFFSET ?")
    statement.setInt(1, offset)                                    // ...the indexed access
    statement.executeQuery()
}

// OPEN
fun boundPageOffsetOpen(connection: Connection, offset: Int) {
    val statement = connection.prepareStatement("SELECT * FROM items LIMIT 20 OFFSET ?")
    statement.setInt(1, offset)                 // offset used with no range check anywhere before it
    statement.executeQuery()
}

// --- realization 7: noun=string builder position, verb=index bound check before deleteCharAt() -
// CLOSED
fun boundStringBuilderDeleteClosed(sb: StringBuilder, index: Int) {
    require(index in 0 until sb.length) { "index out of range" }   // bound check precedes...
    sb.deleteCharAt(index)                                           // ...the indexed access
}

// OPEN
fun boundStringBuilderDeleteOpen(sb: StringBuilder, index: Int) {
    sb.deleteCharAt(index)                      // indexed mutation with no bound check anywhere before it
}

// --- realization 8: noun=random-access file offset, verb=range check before RandomAccessFile.seek() -
// CLOSED
fun boundFileSeekClosed(raf: RandomAccessFile, offset: Long) {
    require(offset in 0..raf.length()) { "offset out of range" }   // bound check precedes...
    raf.seek(offset)                                                 // ...the indexed access
    raf.readByte()
}

// OPEN
fun boundFileSeekOpen(raf: RandomAccessFile, offset: Long) {
    raf.seek(offset)                            // seek with no range check anywhere before it
    raf.readByte()
}
