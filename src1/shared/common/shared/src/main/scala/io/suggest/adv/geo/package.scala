package io.suggest.adv

import io.suggest.adv.rcvr._
import japgolly.univeq.UnivEq
import play.api.libs.json._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 15:22
  */
package object geo {

  type RcvrsMap_t = Map[RcvrKey, Boolean]

  implicit def rcvrMapJson: Format[RcvrsMap_t] = {
    val keyWrites = rcvrKeyKeyWrites
    val delimCh = keyWrites.DELIM.head
    val reads = Reads.mapReads[RcvrKey, Boolean] { k =>
      JsResult.fromTry(
        Try(
          k .split( delimCh )
            .toList
        )
          .filter(_.nonEmpty),
        ex =>
          JsError( ex.toString )
      )
    }
    val writes = Writes.keyMapWrites[RcvrKey, Boolean, Map]( keyWrites, implicitly )
    Format( reads, writes )
  }

  @inline implicit def rcvrsMapUe: UnivEq[RcvrsMap_t] = UnivEq.force

}
