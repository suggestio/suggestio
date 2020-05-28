package io.suggest.swfs.client.proto.lookup

import io.suggest.swfs.client.proto.IToQs
import io.suggest.swfs.fid.IVolumeId

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:17
 * Description: Параметры запроса dir volume lookup.
 */

final case class LookupRequest(
                                override val volumeId: Int,
                              )
  extends IVolumeId with IToQs
{

  override def toQs: String = {
    "?volumeId=" + volumeId
  }

}
