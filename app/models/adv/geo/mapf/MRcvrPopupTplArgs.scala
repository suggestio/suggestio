package models.adv.geo.mapf

import io.suggest.model.n2.node.MNode
import models.MNodeType
import models.mdt.IDateStartEnd
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 17:26
  * Description: Аргументы для рендера шаблона попапа по одному узлу на карте георазмещения карточек.
  *
  * Работа идёт поверх пачки моделей: на первом месте -- маппинг формы, который задаёт весь рендер формы.
  * У формы есть список групп узлов и списки узлов в рамках этих групп.
  * И есть вспомогательная карта данных по узлам.
  */

/** Модель аргументов рендера шаблона popup'а для узла карты георазмещения.
  *
  * @param form Забинденная форма, по которой пойдёт рендер.
  * @param nodeInfos Карта инфы по узлам для нужд рендера каких-то человеческих данных.
  */
case class MRcvrPopupTplArgs(
  form         : Form[MRcvrPopupFormRes],
  nodeInfos    : Map[String, MNodeInfo]
)


/**
  * Модель результата биндинга формы.
  *
  * @param nodeId id узла, для которого отрендерена форма размещения.
  * @param groups Группы узлов размещения внутри текущего узла: саморазмещение, маячки, теги, и т.д.
  */
case class MRcvrPopupFormRes(
  nodeId  : String,
  groups  : List[MNodeAdvGroup]
)


/**
  * Группа узлов по типу для нужд формы попапа.
  *
  * @param ntypeOpt Типы под-узлов в группе, если требуется её обозначать.
  *              None значит, что группа текущего узла.
  * @param nodes Данные по конкретным узлам в рамках формы.
  */
case class MNodeAdvGroup(
  ntypeOpt  : Option[MNodeType],
  nodes     : List[MNodeAdvFormInfo]
)


/** Инфа о узле для маппинга формы.
  *
  * @param nodeId id узла для hidden-поля.
  * @param isCreate Создаваемое размещение? Hidden-поле, для возможности явно создавать или удалять размещения.
  * @param checked Значение галочки во время рендера.
  */
case class MNodeAdvFormInfo(
  nodeId    : String,
  isCreate  : Boolean,
  checked   : Boolean
)


/**
  * Инфа об одном оформленом размещении на узле.
  *
  * @param intervalOpt Период размещения.
  * @param nameOpt Отображаемое имя, если есть. По идее, оно есть всегда, но на всякий пожарный передаём Option.
  * @param isOnlineNow Есть ли хоть одно активное размещение текущей карточки на данном узле?
  */
case class MNodeInfo(
  nameOpt        : Option[String],
  isOnlineNow    : Boolean,
  intervalOpt    : Option[IDateStartEnd]
)

