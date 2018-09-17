package io.suggest.adv.rcvr

import boopickle.Default._
import io.suggest.common.tree.{NodesTreeApiIId, NodesTreeWalk}
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.primo.id.IId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:21
  * Description: Сериализуемые клиент-серверные модели содержимого попапа над ресивером на карте георазмещения.
  */


object MRcvrPopupResp {
  implicit val pickler: Pickler[MRcvrPopupResp] = {
    implicit val mrpgP = IRcvrPopupNode.rcvrPopupGroupP
    generatePickler[MRcvrPopupResp]
  }
}

/** Модель ответа сервера на запрос попапа для ресивера.
  *
  * @param node Рекурсивная модель данных по размещению на узлу и под-узлах, если возможно.
  */
case class MRcvrPopupResp(
                          node: Option[IRcvrPopupNode]
                         )


/** Поддержка для модели узлов и подузлов ресиверов в попапе ресивера. */
object IRcvrPopupNode extends NodesTreeApiIId with NodesTreeWalk {

  override type T = IRcvrPopupNode

  override protected def _subNodesOf(node: IRcvrPopupNode): Iterator[IRcvrPopupNode] = {
    node.subGroups
      .iterator
      .flatMap(_.nodes)
  }

  /**
    * Рекурсивный pickler весь живёт здесь.
    * Пиклер для [[MRcvrPopupNode]] тоже здесь генерится, т.к. иначе всё будет плохо:
    * (иначе будут два пиклера, вызывающие друг-друга в цикле).
    * Рекурсивность пиклера требует наличия интерфейса, поэтому тут интерфейс маппиться на свою единственную реализацию.
    */
  implicit val rcvrPopupGroupP: Pickler[IRcvrPopupNode] = {
    implicit val metaP = MRcvrPopupMeta.rcvrPopupMetaPickler
    implicit val selfP = compositePickler[IRcvrPopupNode]
    selfP.addConcreteType[MRcvrPopupNode]
  }

}

/**
  * Интерфейс для модели [[MRcvrPopupNode]].
  * Рекурсивные типы в boopickle неявно требуют интерфейса, который они будут реализовавывать.
  * Без интерфейса всё молча компилится в пустой pickler, бесконечно вызывающий сам себя.
  */
sealed trait IRcvrPopupNode extends IId[String] {

  /** id узла. */
  override val id     : String

  /** Отображаемое название узла. */
  val name            : Option[String]

  /** Данные по чек-боксу, если надо отображать. */
  val checkbox        : Option[MRcvrPopupMeta]

  /** Подгруппы узлов текущего узла. */
  val subGroups       : Seq[MRcvrPopupGroup]

}


/** Описание узла с данными одного узла в ответе [[MRcvrPopupResp]]. */
case class MRcvrPopupNode(
                           override val id              : String,
                           override val name            : Option[String],
                           override val checkbox        : Option[MRcvrPopupMeta],
                           override val subGroups       : Seq[MRcvrPopupGroup]
                         )
  extends IRcvrPopupNode

object MRcvrPopupMeta {
  implicit val rcvrPopupMetaPickler: Pickler[MRcvrPopupMeta] = {
    implicit val mRangeOptP = MRangeYmdOpt.mRangeYmdOptPickler
    generatePickler[MRcvrPopupMeta]
  }
}
/** Метаданные узла для размещения прямо на нём.
  *
  * @param isCreate true -- ожидается создание размещения.
  *                 false -- возможно удаление текущего размещения.
  * @param checked Исходное состояние галочки.
  * @param isOnlineNow Сейчас активен?
  * @param dateRange Диапазон дат размещения, если есть.
  *                  Если содержит хоть какое-то значение внутри, значит уже есть проплаченное срочное размещение.
  */
case class MRcvrPopupMeta(
                           isCreate        : Boolean,
                           checked         : Boolean,
                           isOnlineNow     : Boolean,
                           dateRange       : MRangeYmdOpt
                         )


/** Группа под-узлов.
  *
  * @param title Отображаемый заголовок группы, если требуется.
  * @param nodes Узлы данной группы, которые могут содержать подгруппы [[MRcvrPopupGroup]].
  */
case class MRcvrPopupGroup(
                            title  : Option[String],
                            nodes  : Seq[IRcvrPopupNode]
                          )
