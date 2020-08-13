package io.suggest.lk.nodes

import io.suggest.scalaz.ZTreeUtil.{ZTREE_FORMAT, zTreeUnivEq}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:38
  * Description: Кроссплатформенная модель инфы о дереве узлов.
  * Рекурсия на уровне модели ленивая, т.е. верхний слой узлов может не содержать под-узлов.
  * На раннем этапе: это достаточно для списка маячков.
  * На последующих этапах: подгружать под-узлы с сервера по мере погружения юзера в глубины иерархии.
  */

object MLknNodeResp {

  @inline implicit def univEq: UnivEq[MLknNodeResp] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mLknNodeRespFormat: OFormat[MLknNodeResp] = {
    (__ \ "i").format[Tree[MLknNode]]
      .inmap[MLknNodeResp]( apply, _.subTree )
  }

}

/**
  * Класс модели ответа на запрос под-списка узлов.
  * @param subTree Инфа по узлу и его под-узлам.
  */
case class MLknNodeResp(
                         subTree    : Tree[MLknNode],
                       )
