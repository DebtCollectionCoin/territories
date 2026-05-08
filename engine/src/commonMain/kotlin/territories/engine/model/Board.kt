package territories.engine.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = BoardSerializer::class)
class Board private constructor(
    val cols: Int,
    val rows: Int,
    private val cells: Array<Array<Cell>>   // cells[col][row]
) {
    companion object {
        fun empty(cols: Int, rows: Int): Board {
            val cells = Array(cols) { Array(rows) { Cell() } }
            return Board(cols, rows, cells)
        }

        fun from(cols: Int, rows: Int, cells: Array<Array<Cell>>): Board =
            Board(cols, rows, cells)
    }

    fun get(coord: Coord): Cell = cells[coord.col][coord.row]

    fun isOnBoard(coord: Coord): Boolean =
        coord.col in 0 until cols && coord.row in 0 until rows

    fun isBorder(coord: Coord): Boolean =
        coord.col == 0 || coord.row == 0 ||
        coord.col == cols - 1 || coord.row == rows - 1

    /** Returns a new Board with the dot placed at [coord] for [player]. */
    fun withDot(coord: Coord, player: Player): Board {
        val newCells = copyCells()
        newCells[coord.col][coord.row] = newCells[coord.col][coord.row].copy(dot = player)
        return Board(cols, rows, newCells)
    }

    /** Returns a new Board with all [coords] claimed as territory for [owner]. */
    fun withTerritory(coords: Set<Coord>, owner: Player): Board {
        val newCells = copyCells()
        for (coord in coords) {
            newCells[coord.col][coord.row] = newCells[coord.col][coord.row].copy(territory = owner)
        }
        return Board(cols, rows, newCells)
    }

    fun allCoords(): Sequence<Coord> = sequence {
        for (col in 0 until cols) {
            for (row in 0 until rows) {
                yield(Coord(col, row))
            }
        }
    }

    fun cellsOf(player: Player): List<Coord> =
        allCoords().filter { get(it).dot == player }.toList()

    fun territoryCells(owner: Player): List<Coord> =
        allCoords().filter { get(it).territory == owner }.toList()

    private fun copyCells(): Array<Array<Cell>> =
        Array(cols) { col -> Array(rows) { row -> cells[col][row] } }

    // For serialization use
    internal fun rawCells(): Array<Array<Cell>> = cells
}

/** Serializes Board as {cols, rows, cells: [flat list of Cell]} */
object BoardSerializer : KSerializer<Board> {
    private val cellListSerializer = ListSerializer(Cell.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Board") {
        element<Int>("cols")
        element<Int>("rows")
        element<List<Cell>>("cells")
    }

    override fun serialize(encoder: Encoder, value: Board) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.cols)
            encodeIntElement(descriptor, 1, value.rows)
            val flat = mutableListOf<Cell>()
            for (col in 0 until value.cols) {
                for (row in 0 until value.rows) {
                    flat.add(value.get(Coord(col, row)))
                }
            }
            encodeSerializableElement(descriptor, 2, cellListSerializer, flat)
        }
    }

    override fun deserialize(decoder: Decoder): Board {
        return decoder.decodeStructure(descriptor) {
            var cols = 0; var rows = 0; var flat: List<Cell> = emptyList()
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> cols = decodeIntElement(descriptor, 0)
                    1 -> rows = decodeIntElement(descriptor, 1)
                    2 -> flat = decodeSerializableElement(descriptor, 2, cellListSerializer)
                    kotlinx.serialization.encoding.CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }
            val cells = Array(cols) { col -> Array(rows) { row -> flat[col * rows + row] } }
            Board.from(cols, rows, cells)
        }
    }
}
