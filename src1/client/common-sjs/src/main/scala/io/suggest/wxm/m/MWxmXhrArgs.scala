package io.suggest.wxm.m

import java.nio.ByteBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.07.17 22:13
  * Description:
  */
trait IWxmXhrArgs[Out] {

  def payload: Out

  def payloadBytes: ByteBuffer

}


case class MWxmXhrArgs[Out](
                             override val payload       : Out,
                             override val payloadBytes  : ByteBuffer
                           )
  extends IWxmXhrArgs[Out]
