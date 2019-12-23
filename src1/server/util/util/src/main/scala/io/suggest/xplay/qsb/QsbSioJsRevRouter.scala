package io.suggest.xplay.qsb

import io.suggest.common.qs.QsConstants
import play.api.mvc.QueryStringBindable

/** Использовать навороченный вариант javascript Unbind из sio jsRevRouterTpl. */
trait QsbSioJsRevRouter[T] extends QueryStringBindable[T] {
  override def javascriptUnbind: String = {
    QsConstants.JSRR_OBJ_TO_QS_F
  }
}
