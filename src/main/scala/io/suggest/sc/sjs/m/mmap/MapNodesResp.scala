package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.map.ScMapConstants.Nodes._
import io.suggest.sjs.common.geo.json.GjFeatureCollection
import io.suggest.sjs.common.model.{Timestamped, TimestampedCompanion}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 12:12
  * Description: Модель ответа по карте узлов.
  */
@js.native
sealed class MapNodesResp extends js.Object {
  @JSName( SOURCES_FN )
  var sources: js.Array[MapNodesSource] = js.native
}


/** Интерфейс для данных одного источника. */
@js.native
sealed class MapNodesSource extends js.Object {

  @JSName( SRC_NAME_FN )
  var name: String = js.native

  @JSName( IS_CLUSTERED_FN )
  var isClustered: Boolean = js.native

  @JSName( SRC_DATA_FN )
  var data: GjFeatureCollection = js.native

}


/**
  * Timestamped-обертка над инстансами [[MapNodesResp]].
  *
  * @param result Try для [[MapNodesResp]].
  * @param timestamp Время начала запроса.
  */
case class MapNodesRespTs(
  override val result     : Try[MapNodesResp],
  override val timestamp  : Long
)
  extends Timestamped[MapNodesResp]
object MapNodesRespTs
  extends TimestampedCompanion[MapNodesResp]
