package util.geo.osm

import enumeratum.values.{NoSuchMember, StringEnum, StringEnumEntry, ValueEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.05.2020 15:42
  */

object RelMemberRoles extends StringEnum[RelMemberRole] {

  // Нужен upper case
  case object INNER extends RelMemberRole("inner")
  case object OUTER extends RelMemberRole("outer")
  case object ADMIN_CENTRE extends RelMemberRole("admin_centre")
  case object LABEL extends RelMemberRole("label")
  case object SUBAREA extends RelMemberRole("subarea")


  override def values = findValues


  def default = OUTER


  lazy val allValuesMap: Map[String, RelMemberRole] = {
    (for {
      v <- values.iterator
      value <- v.allValues
    } yield {
      value -> v
    })
      .toMap
  }

  override def withValueOpt(i: String): Option[RelMemberRole] =
    allValuesMap.get(i)

  // Этот метод не используется, но из-за смены values map, он выдаёт результаты, не соответствующие withValue()-методам.
  override def withValueEither(i: String): Either[NoSuchMember[String, ValueEnumEntry[String]], RelMemberRole] =
    throw new UnsupportedOperationException("Not re-implemented for values map (unused).")

}


sealed abstract class RelMemberRole(override val value: String) extends StringEnumEntry

object RelMemberRole {

  @inline implicit def univEq: UnivEq[RelMemberRole] = UnivEq.derive

  implicit final class RmrOpsExt( private val rmr: RelMemberRole ) extends AnyVal {

    def isRelationBorder: Boolean = {
      rmr match {
        case RelMemberRoles.OUTER | RelMemberRoles.INNER => true
        case _ => false
      }
    }

    def allValues: List[String] = {
      val v0 = rmr.value :: Nil
      rmr match {
        case RelMemberRoles.INNER => "enclave" :: v0
        case RelMemberRoles.OUTER => "exclave" :: "" :: v0
        case _ => v0
      }
    }

  }

}
