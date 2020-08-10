package io.suggest.lk.nodes.form.m

import diode.data.{Pot, Ready}
import io.suggest.common.tree.{NodeTreeUpdate, NodesTreeApiIId, NodesTreeWalk}
import io.suggest.lk.nodes.{MLknNode, MLknNodeResp}
import io.suggest.primo.id.IId
import io.suggest.log.Log
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.msg.ErrorMsgs
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 16:36
  * Description: Модель рантаймового состояния одного узла в списке узлов дерева.
  */

object MNodeState
  extends NodesTreeApiIId
    with NodesTreeWalk
    with NodeTreeUpdate
    with Log
{

  override type T = MNodeState

  override protected def _subNodesOf(node: MNodeState): IterableOnce[MNodeState] = {
    node.children.getOrElse(Nil)
  }

  def apply(resp: MLknNodeResp): MNodeState = {
    MNodeState(
      info = resp.info,
      children = {
        if (resp.children.isEmpty) {
          // А если дочерних узлов не существует, то children надо пропатчить через .withChildren() самостоятельно.
          Pot.empty
        } else {
          val chs = for (ch <- resp.children) yield {
            MNodeState(ch)
          }
          Ready(chs)
        }
      }
    )
  }

  override def withNodeChildren(node: MNodeState, children2: IterableOnce[MNodeState]): MNodeState = {
    // Хз, надо ли проверять Pot.empty. Скорее всего, этот метод никогда не вызывается для Empty Pot.
    if (node.children.isEmpty) {
      logger.warn( ErrorMsgs.REFUSED_TO_UPDATE_EMPTY_POT_VALUE, msg = node + SPACE + children2 )
      node
    } else {
      (MNodeState.children set Ready( children2.toSeq ) )(node)
    }
  }

  @inline implicit def univEq: UnivEq[MNodeState] = {
    import io.suggest.ueq.JsUnivEqUtil._
    UnivEq.derive
  }

  def info        = GenLens[MNodeState](_.info)
  def children    = GenLens[MNodeState](_.children)
  def isEnableUpd = GenLens[MNodeState](_.isEnabledUpd)
  def editing     = GenLens[MNodeState](_.editing)
  def tfInfoWide  = GenLens[MNodeState](_.tfInfoWide)
  def adv         = GenLens[MNodeState](_.adv)

}


/** Класс модели рантаймового состояния одного узла в списке узлов.
  *
  * @param info Данные по узлу, присланные сервером.
  * @param children Состояния дочерних элементов в натуральном порядке.
  *                 Элементы запрашиваются с сервера по мере необходимости.
  */
case class MNodeState(
                       info               : MLknNode,
                       children           : Pot[Seq[MNodeState]]              = Pot.empty,
                       isEnabledUpd       : Option[MNodeEnabledUpdateState]   = None,
                       editing            : Option[MEditNodeState]            = None,
                       tfInfoWide         : Boolean                           = false,
                       adv                : Option[MNodeAdvState]             = None
                     )
  extends IId[String]
{

  override def id = info.id

  /** Является ли текущее состояние узла нормальным и обычным?
    *
    * @return true, значит можно отрабатывать клики по заголовку в нормальном режиме.
    *         false -- происходит какое-то действо, например переименование узла.
    */
  def isNormal = editing.isEmpty && !advIsPending

  def advIsPending = adv.exists(_.newIsEnabledPot.isPending)

}

