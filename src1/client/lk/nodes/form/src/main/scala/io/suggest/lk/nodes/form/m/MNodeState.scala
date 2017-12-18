package io.suggest.lk.nodes.form.m

import diode.data.{Pot, Ready}
import io.suggest.common.tree.{NodeTreeUpdate, NodesTreeApiIId, NodesTreeWalk}
import io.suggest.lk.nodes.{MLknNode, MLknNodeResp}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.Log
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 16:36
  * Description: Модель рантаймового состояния одного узла в списке узлов дерева.
  */


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

  def withInfo(info2: MLknNode) = copy(info = info2)
  def withChildren(children2: Pot[Seq[MNodeState]]) = copy(children = children2)
  def withNodeEnabledUpd(neu: Option[MNodeEnabledUpdateState]) = copy(isEnabledUpd = neu)
  def withEditing(editing2: Option[MEditNodeState]) = copy(editing = editing2)
  def withAdv(adv2: Option[MNodeAdvState] = None) = copy(adv = adv2)
  def withTfInfoWide(tiw: Boolean) = copy(tfInfoWide = tiw)

  /** Является ли текущее состояние узла нормальным и обычным?
    *
    * @return true, значит можно отрабатывать клики по заголовку в нормальном режиме.
    *         false -- происходит какое-то действо, например переименование узла.
    */
  def isNormal = editing.isEmpty && !advIsPending

  def advIsPending = adv.exists(_.req.isPending)

}


object MNodeState
  extends NodesTreeApiIId
  with NodesTreeWalk
  with NodeTreeUpdate
  with Log
{

  override type T = MNodeState

  override protected def _subNodesOf(node: MNodeState): TraversableOnce[MNodeState] = {
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

  override def withNodeChildren(node: MNodeState, children2: TraversableOnce[MNodeState]): MNodeState = {
    // Хз, надо ли проверять Pot.empty. Скорее всего, этот метод никогда не вызывается для Empty Pot.
    if (node.children.isEmpty) {
      LOG.warn( WarnMsgs.REFUSED_TO_UPDATE_EMPTY_POT_VALUE, msg = node + SPACE + children2 )
      node
    } else {
      node.withChildren(
        Ready( children2.toSeq )
      )
    }
  }

}
