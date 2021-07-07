package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknNode, MLknNodeReq, MLknNodeResp, MLknOpKey, MLknOpValue}
import io.suggest.n2.node.MNodeType
import io.suggest.nfc.NfcPendingState
import io.suggest.radio.MRadioData
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

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
  * @param nodeType Fixed type of node to create. Usually, related to fixed defined id field.
  * @param parentPath Путь до родительского узла в текущем дереве узлов.
  * @param id Фиксированный идентификатор создаваемого узла.
  * @param nameDflt Дефолтовое название маячка.
  */
case class CreateNodeClick(
                            nodeType: Option[MNodeType] = None,
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

/** Changing node type for creating node. */
case class CreateNodeTypeChange( nodeType: MNodeType )
  extends LkNodesAction

/** Смена узла-родителя в селекте. */
case class CreateNodeParentChange( nodePath: NodePath_t )
  extends LkNodesAction


/** Добавление под-узла: Юзер нажимает "сохранить". */
case object CreateNodeSaveClick
  extends LkNodesAction


/** Среагировать на ответ сервера по поводу успешного добавления нового узла. */
case class CreateNodeResp(
                           parentPath : NodePath_t,
                           parentRk   : RcvrKey,
                           req        : MLknNodeReq,
                           tryResp    : Try[MLknNode],
                         )
  extends LkNodesAction


/** Добавление под-узла: юзер нажал "отмена". */
case object CreateNodeCloseClick
  extends LkNodesAction


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


// Управление размещением текущей карточки на указанном узле.

/** Унифицированный экшен для управления флагами в обе стадии.
  *
  * @param nodePath Путь до узла в дереве на экране.
  *                 None - означает, что смотреть надо в tree.opened.
  * @param key Ключ.
  * @param value Контейнер нового значения флага.
  * @param nextPot Состояние.
  * @param nodeRk RcvrKey для контроля корректности nodePath на асинхронных операциях с деревом.
  */
case class ModifyNode(
                       nodePath     : Option[NodePath_t]      = None,
                       key          : MLknOpKey,
                       value        : MLknOpValue,
                       nextPot      : Pot[MLknNode]           = Pot.empty,
                       nodeRk       : Option[RcvrKey]         = None,
                     )
  extends LkNodesAction


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


/** Отмена редактирования тарифа текущего узла. */
case object TfDailyCancelClick
  extends LkNodesAction

/** Юзер редактирует значением amount для ручного тарифа. */
case class TfDailyManualAmountChanged(amount: String)
  extends LkNodesAction

case class TfDailyModeChanged( modeId: String ) extends LkNodesAction


/** Экшены Beacons-контроллера. */
sealed trait ILknBeaconsAction extends LkNodesAction

/** Обнаружены маячки. */
case class BeaconsDetected( beacons: Map[String, MRadioData] ) extends ILknBeaconsAction

/** Ответ сервера с инфой по маячкам. */
case class BeaconsScanResp( reqArgs: MLknBeaconsScanReq, tryResp: Try[MLknNodeResp] ) extends ILknBeaconsAction

/** Выполнить рендер. */
case object BeaconsRenderTimer extends ILknBeaconsAction

/** Переключение формы между управлением узлами и размещением рекламной карточки на лету. */
case class SetAd( adId: Option[String] ) extends LkNodesAction


/** Interface for actions for NFC-controller. */
sealed trait INfcWriteAction extends LkNodesAction

/** Open NFC writer dialog. */
case class NfcDialog( isOpen: Boolean ) extends INfcWriteAction


/** Confirm/cancel NFC writing.
  *
  * @param op Requested NFC Operation.
  *           May be None for cancel/stop any current operation.
  * @param state Progressing steps.
  *              unavailable() means cancelling of operation.
  */
case class NfcWrite(
                     op     : Option[MNfcOperation],
                     state  : Pot[NfcPendingState]    = Pot.empty,
                   )
  extends INfcWriteAction
object NfcWrite {
  @inline implicit def univEq: UnivEq[NfcWrite] = UnivEq.derive
  def state = GenLens[NfcWrite](_.state)
  def cancel = apply( None, Pot.empty.unavailable() )
}

// TODO NfcModeSet(mode: Adv | AdnNode)
