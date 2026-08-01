package org.genspectrum.lapis.silonew

/** Column types reported by SILO's `schema()` operation. */
enum class SiloColumnType {
    STRING,
    INDEXED_STRING,
    DATE32,
    BOOL,
    INT32,
    INT64,
    FLOAT,
}

/** Builds a schema whose tables can be used as source relations in SILO queries. */
fun siloSchema(block: SiloSchemaBuilder.() -> Unit): SiloSchema = SiloSchemaBuilder().apply(block).build()

class SiloSchema internal constructor(
    tables: List<SiloTable>,
) {
    private val tablesByName = tables.associateBy(SiloTable::name)

    val tables: List<SiloTable> = tables.toList()

    fun table(name: String): SiloTable =
        tablesByName[name] ?: throw IllegalArgumentException("Table '$name' is not declared in this SILO schema")

    operator fun get(name: String): SiloTable = table(name)

    override fun equals(other: Any?) = other is SiloSchema && tables == other.tables

    override fun hashCode() = tables.hashCode()

    override fun toString() = "SiloSchema(tables=$tables)"
}

class SiloSchemaBuilder {
    private val tables = linkedMapOf<String, SiloTable>()

    fun table(
        name: String,
        block: SiloTableBuilder.() -> Unit = {},
    ) {
        require(name !in tables) { "Table '$name' is declared more than once" }
        tables[name] = SiloTableBuilder(name).apply(block).build()
    }

    internal fun build() = SiloSchema(tables.values.toList())
}

class SiloTableBuilder internal constructor(
    private val name: String,
) {
    private val columns = linkedMapOf<String, SiloColumnType>()

    fun column(
        name: String,
        type: SiloColumnType,
    ) {
        require(name !in columns) { "Column '$name' is declared more than once in table '${this.name}'" }
        columns[name] = type
    }

    internal fun build() = SiloTable(name = name, columns = columns.entries.map { it.toPair() })
}

class SiloTable internal constructor(
    val name: String,
    columns: List<Pair<String, SiloColumnType>>,
) : SiloRelation() {
    private val columnsByName = columns.associate { (name, type) ->
        name to SiloColumn(table = this, name = name, type = type)
    }

    val columns: List<SiloColumn> = columnsByName.values.toList()

    fun column(name: String): SiloColumn =
        columnsByName[name]
            ?: throw IllegalArgumentException("Column '$name' is not declared in SILO table '${this.name}'")

    operator fun get(name: String): SiloColumn = column(name)

    internal override val precedence = ATOMIC_PRECEDENCE

    internal override fun renderSelf() = renderIdentifier(name)

    override fun equals(other: Any?) = other is SiloTable && name == other.name && columns == other.columns

    override fun hashCode() = 31 * name.hashCode() + columns.hashCode()

    override fun toString() = "SiloTable(name=$name, columns=$columns)"
}

class SiloColumn internal constructor(
    val table: SiloTable,
    override val name: String,
    val type: SiloColumnType,
) : SiloField(name) {
    internal override fun renderSelf() = renderQuotedIdentifier(name)

    override fun equals(other: Any?) =
        other is SiloColumn && table.name == other.table.name && name == other.name && type == other.type

    override fun hashCode() = 31 * (31 * table.name.hashCode() + name.hashCode()) + type.hashCode()

    override fun toString() = "SiloColumn(table=${table.name}, name=$name, type=$type)"
}
