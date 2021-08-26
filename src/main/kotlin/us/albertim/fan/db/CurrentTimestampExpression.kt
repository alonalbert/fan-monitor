package us.albertim.fan.db

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.QueryBuilder

class CurrentTimestampExpression : Expression<Long>() {
  override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append("strftime('%s','now')") }
}