package io.suggest.swfs.client.proto

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
      src.charAt(i).toInt
    }

    Replication(
      otherDc     = _parseArg(0),
      otherRack   = _parseArg(1),
      sameRack    = _parseArg(2)
    )
  }

}


case class Replication(
  otherDc   : Int = 0,
  otherRack : Int = 0,
  sameRack  : Int = 0
) {
  override def toString = s"$otherDc$otherRack$sameRack"
}
