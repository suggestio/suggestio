package io.suggest.sc.sc3

import io.suggest.text.StringUtil
import io.suggest.xplay.json.PlayJsonUtil
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 14:56
  * Description: Модель-контейнер данных с ответом сервера по поводу выдачи.
  *
  * Сервер возвращает различные данные для разных экшенов, но всегда в одном формате.
  * Это неявно-пустая модель.
  */
object MSc3Resp {

  /** Поддержка play-json. */
  implicit def msc3RespFormat: OFormat[MSc3Resp] = {
    (__ \ "ra")
      .format[Seq[MSc3RespAction]] {
        PlayJsonUtil.readsSeqNoErrorFormat[MSc3RespAction]
      }
      .inmap[MSc3Resp]( apply, _.respActions )
  }

  @inline implicit def univEq: UnivEq[MSc3Resp] = UnivEq.derive

}


/** Класс-контейнер ответа сервера для выдачи sc3.
  *
  * @param respActions List-список данных resp-экшенов, которые выдача должна применить к своему состоянию.
  *                    И List, и порядок имеют значение: экшены применяются выдачей в исходном порядке.
  */
case class MSc3Resp(
                     respActions: Seq[MSc3RespAction],
                   ) {

  /** Сравнить тип первого экшена с указанным значением. */
  def isNextActionType(expected: MScRespActionType): Boolean = {
    respActions.nonEmpty && {
      val ra0 = respActions.head
      ra0.acType ==* expected
    }
  }

  /** Множество всех типов экшенов, упомянутых в respActions. */
  lazy val respActionTypes: Set[MScRespActionType] = {
    respActions
      .iterator
      .map(_.acType)
      .toSet
  }

  override def toString: String = {
    StringUtil.toStringHelper( this, 512 ) { renderF =>
      if (respActions.nonEmpty)
        renderF("")(respActions.mkString("[\n", ",\n ", "\n]"))
    }
  }

}
