package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.ble.BeaconsNearby_t
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknNode, MLknNodeResp}
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:48
  * Description: Экшены формы управления узлами.
  */
sealed trait LkNodesAction extends DAction

/** Экшен, связанный с деревом узлов. Он всега содержит поле rcvrKey. */
sealed trait LkNodesTreeAction
  extends LkNodesAction
{
  def nodePath: NodePath_t
}

sealed trait LkNodesTreeNameAction extends LkNodesTreeAction {
  val name: String
}

sealed trait INodeUpdatedResp extends LkNodesAction {
  /** id узла, если есть. */
  def nodeIdOpt: Option[String]
  /** @return None - узел удалён.
    *         Some() - узел обновлён на указанное состояние.
    */
  def nodeUpdated: Try[Option[MLknNode]]
}

/** Юзер кликнул по узлу, необходимо развернуть узел. */
case class NodeClick(
                      override val nodePath: NodePath_t,
                    )
  extends LkNodesTreeAction


/** Экшен инициализации дерева с сервера.
  * Вынесен из NodeClick(nodePath=Nil) конфликтует с корнем дерева, а в коде они всё-равно живут отдельно. */
case class TreeInit( treePot: Pot[MLknNodeResp] = Pot.empty ) extends LkNodesAction


/** Модель результата запроса к серверу по поводу под-узлов для указанного узла. */
case class HandleSubNodesOf(
                             override val nodePath  : NodePath_t,
                             subNodesRespTry        : Try[MLknNodeResp],
                             tstampMs               : Long,
                           )
  extends LkNodesTreeAction


/** Юзер кликнул по кнопке добавления подузла.
  *
  * @param parentPath Путь до родительского узла в текущем дереве узлов.
  * @param id Фиксированный идентификатор создаваемого узла.
  * @param nameDflt Дефолтовое название маячка.
  */
case class CreateNodeClick(
                            parentPath: Option[NodePath_t] = None,
                            id: Option[String] = None,
                            nameDflt: Option[String] = None,
                          )
  extends LkNodesAction



/** Добавление под-узла: Юзер вводит название узла (маячка). */
case class CreateNodeNameChange( name: String )
  extends LkNodesAction


/** Добавление под-узла: Юзер вводит id узла (маячка). */
case class CreateNodeIdChange( id: String )
  extends LkNodesAction

/** Смена узла-родителя в селекте. */
case class CreateNodeParentChange( nodePath: NodePath_t )
  extends LkNodesAction


/** Добавление под-узла: Юзер нажимает "сохранить". */
case object CreateNodeSaveClick
  extends LkNodesAction


/** Среагировать на ответ сервера по поводу успешного добавления нового узла. */
case class CreateNodeResp(tryResp: Try[MLknNode] )
  extends LkNodesAction
  with INodeUpdatedResp
{
  override lazy val nodeUpdated = tryResp.map(Some.apply)
  override def nodeIdOpt = tryResp.toOption.map(_.id)
}


/** Добавление под-узла: юзер нажал "отмена". */
case object CreateNodeCloseClick
  extends LkNodesAction



/** Сигнал о клике по галочке узла. */
case class NodeIsEnabledChanged(
                                 isEnabled            : Boolean,
                               )
  extends LkNodesAction


/** Сигнал ответа сервера на апдейт флага isEnabled. */
case class NodeIsEnabledUpdateResp(
                                    override val nodePath: NodePath_t,
                                    nodeId               : String,
                                    resp                 : Try[MLknNode]
                                  )
  extends LkNodesTreeAction
  with INodeUpdatedResp
{
  override def nodeIdOpt = Some( nodeId )
  override def nodeUpdated = resp.map(Some.apply)
}


// Удалятельство узлов.

/** Клик по кнопке удаления узла. */
case object NodeDeleteClick
  extends LkNodesAction


// Выпадающая менюшка узла с доп.функциями.
/** Клик по кнопке меню узла. */
case object NodeMenuBtnClick
  extends LkNodesAction


/** Результат запроса к серверу по поводу удаления узла. */
case class NodeDeleteResp(
                           override val nodePath: NodePath_t,
                           nodeId: String,
                           resp: Try[Boolean]
                         )
  extends LkNodesTreeAction
  with INodeUpdatedResp
{
  override def nodeIdOpt = Some(nodeId)
  override def nodeUpdated = resp.map(_ => None)
}


// Редактирование узла (переименование).

/** Клик по кнопке редактирования узла в дереве узлов. */
case object NodeEditClick extends LkNodesAction

/** Редактирование названия узла: Юзер вводит название узла (маячка). */
case class NodeEditNameChange( name: String ) extends LkNodesAction

/** Клик по кнопке отмены редактирования узла. */
case object NodeEditCancelClick extends LkNodesAction

/** Клик по кнопке подтверждения редактирования узла. */
case object NodeEditOkClick extends LkNodesAction

/** Ответ сервера по итогам запроса. */
case class NodeEditSaveResp(
                             nodeId: String,
                             tryResp: Try[MLknNode]
                           )
  extends LkNodesAction
  with INodeUpdatedResp
{
  override def nodeIdOpt = Some(nodeId)
  override def nodeUpdated = tryResp.map(Some.apply)
}


// Управление размещением текущей карточки на указанном узле.

/** Изменилось состояние галочки, управляющей размещением текущей карточки на узле. */
case class AdvOnNodeChanged(override val nodePath: NodePath_t, isEnabled: Boolean)
  extends LkNodesTreeAction
/** Ответ сервера на тему управления размещением карточки на узле. */
case class AdvOnNodeResp(
                          override val nodePath: NodePath_t,
                          tryResp: Try[MLknNode]
                        )
  extends LkNodesTreeAction


// Управление тарифом узла.
case object TfDailyShowDetails extends LkNodesAction

/** Клик по ссылке редактирования тарифа текущего узла. */
case object TfDailyEditClick
  extends LkNodesAction

/** Команда сохранения возможно обновлённого тарифа. */
case object TfDailySaveClick
  extends LkNodesAction
/** Сигнал ответа сервера по поводу обновления тарифа. */
case class TfDailySavedResp(tryResp: Try[MLknNode])
  extends LkNodesAction
  with INodeUpdatedResp
{
  override def nodeIdOpt = tryResp.toOption.map(_.id)
  override def nodeUpdated = tryResp.map(Some.apply)
}


/** Отмена редактирования тарифа текущего узла. */
case object TfDailyCancelClick
  extends LkNodesAction

/** Юзер редактирует значением amount для ручного тарифа. */
case class TfDailyManualAmountChanged(amount: String)
  extends LkNodesAction

case class TfDailyModeChanged( modeId: String ) extends LkNodesAction

/** Изменилось значение галочки раскрытости карточки по дефолту. */
case class AdvShowOpenedChange( isChecked: Boolean ) extends LkNodesAction

/** Ответ сервера на запрос изменения отображения рекламной карточки. */
case class AdvShowOpenedChangeResp(
                                    override val nodePath: NodePath_t,
                                    reason: AdvShowOpenedChange,
                                    tryResp: Try[_],
                                  )
  extends LkNodesTreeAction

/** Изменение галочки постоянной обводки. */
case class AlwaysOutlinedSet( isChecked: Boolean ) extends LkNodesAction
case class AlwaysOutlinedResp(
                               override val nodePath: NodePath_t,
                               reason: AlwaysOutlinedSet,
                               tryResp: Try[_],
                             )
  extends LkNodesTreeAction


/** Экшены Beacons-контроллера. */
sealed trait ILknBeaconsAction extends LkNodesAction
/** Обнаружены маячки. */
case class BeaconsDetected( beacons: BeaconsNearby_t ) extends ILknBeaconsAction
/** Ответ сервера с инфой по маячкам. */
case class BeaconsScanResp( reqArgs: MLknBeaconsScanReq, tryResp: Try[MLknNodeResp] ) extends ILknBeaconsAction


/** Переключение формы между управлением узлами и размещением рекламной карточки на лету. */
case class SetAd( adId: Option[String] ) extends LkNodesAction
