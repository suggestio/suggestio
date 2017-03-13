package io.suggest.lk.nodes.form.m

import diode.data.{Pot, Ready}
import io.suggest.common.tree.{NodeTreeUpdate, NodesTreeApiIId, NodesTreeWalk}
import io.suggest.lk.nodes.MLknNode
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.common.html.HtmlConstants.SPACE

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
  * @param deleting Происходит процедура удаления: диалоговое окно с подтверждением или даже запрос удаления.
  */
case class MNodeState(
                       info               : MLknNode,
                       children           : Pot[Seq[MNodeState]]              = Pot.empty,
                       isEnabledUpd       : Option[MNodeEnabledUpdateState]   = None,
                       addSubNodeState    : Option[MAddSubNodeState]          = None,
                       deleting           : Option[Pot[_]]                    = None,
                       editing            : Option[MEditNodeState]            = None
                     )
  extends IId[String]
{

  override def id = info.id

  def withInfo(info2: MLknNode) = copy(info = info2)
  def withChildren(children2: Pot[Seq[MNodeState]]) = copy(children = children2)
  def withNodeEnabledUpd(neu: Option[MNodeEnabledUpdateState]) = copy(isEnabledUpd = neu)
  def withAddSubNodeState(asns: Option[MAddSubNodeState]) = copy(addSubNodeState = asns)
  def withDeleting(deleting2: Option[Pot[_]]) = copy(deleting = deleting2)
  def withEditing(editing2: Option[MEditNodeState]) = copy(editing = editing2)

  /** Раскрыт ли сейчас данный узел? */
  def isNodeOpened = children.nonEmpty

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
