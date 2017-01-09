package io.suggest.swfs.client.proto

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 22:40
 * Description: Настройка репликации для seaweedfs.
 *
 * 000: no replication
 * 001: replicate once on the same rack
 * 010: replicate once on a different rack, but same data center
 * 100: replicate once on a different data center
 * 200: replicate twice on two different data center
 * 110: replicate once on a different rack, and once on a different data center
 */
object Replication {

  /** Распарсить значение репликации. */
  def apply(src: String): Replication = {
    if (src.length != 3)
      throw new IllegalArgumentException("unknown replication format: " + src)

    def _parseArg(i: Int): Int = {
      src.charAt(i).toString.toInt
    }

    Replication(
      otherDc     = _parseArg(0),
      otherRack   = _parseArg(1),
      sameRack    = _parseArg(2)
    )
  }


  /** JSON-десериализация из JsString. */
  val READS = Reads[Replication] {
    case JsString(r) =>
      try {
        JsSuccess( apply(r) )
      } catch {
        case ex: Throwable =>
          JsError("expected.replication.format")
      }
    case other =>
      JsError( "expected.jsstring" )
  }

  /** Сериализация в JsString. */
  val WRITES = Writes[Replication] { r =>
    JsString( r.toString )
  }

  /** Поддержка JSON. */
  implicit val FORMAT = Format(READS, WRITES)

}


case class Replication(
  otherDc   : Int = 0,
  otherRack : Int = 0,
  sameRack  : Int = 0
) {

  override def toString: String = {
    s"$otherDc$otherRack$sameRack"
  }

}
