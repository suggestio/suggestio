package io.suggest.sjs.common.async

import scala.concurrent.ExecutionContextExecutor
import scala.scalajs.concurrent.JSExecutionContext.queue

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.07.16 14:01
  * Description: Утиль для асинхронных действия в рамках Scala.js.
  */
object AsyncUtil {

  /**
    * Дефолтовый ExecutionContext вынесен из конкретных реализаций после runNow deprecation.
    * Чтобы в будущем можно было одним взмахом менять execution context.
    *
    * @see [[https://github.com/scala-js/scala-js/issues/2102]]
    */
  @inline
  implicit def defaultExecCtx: ExecutionContextExecutor = queue

}
