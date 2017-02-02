package io.suggest.adv.rcvr

import boopickle.Default._
import io.suggest.dt.interval.MRangeYmdOpt

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
object IRcvrPopupNode {

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

  /** Рекурсивный поиск под-узла по указанному пути id.
    * @param rcvrKey Путь id'шников узла.
    * @param parent Начальный узел.
    * @return Опционально: найденный под-узел.
    *         Если путь пустой, то будет возвращён текущий узел.
    */
  def findSubNode(rcvrKey: RcvrKey, parent: IRcvrPopupNode): Option[IRcvrPopupNode] = {
    if (rcvrKey.isEmpty) {
      Some(parent)
    } else {
      val childRcvrKey = rcvrKey.tail
      parent.subGroups
        .iterator
        .flatMap(_.nodes)
        .filter { _.nodeId == rcvrKey.head }
        .flatMap { subNode =>
          findSubNode(childRcvrKey, subNode)
        }
        .toStream
        .headOption
    }
  }

  /**
    * Строгий поиск узла по указанному node-id пути.
    * В поиске участвует текущий узел и его под-узлы.
    * @param rcvrKey Ключ узла.
    * @param node Начальный узел.
    * @return Опционально: найденный узел.
    */
  def findNode(rcvrKey: RcvrKey, node: IRcvrPopupNode): Option[IRcvrPopupNode] = {
    // Случай пустого rcvrKey НЕ игнорируем, т.к. это скорее защита от самого себя.
    if ( rcvrKey.headOption.contains(node.nodeId) )
      findSubNode(rcvrKey.tail, node)
    else
      None
    // TODO Нужен test надо для этого метода.
  }

}

/**
  * Интерфейс для модели [[MRcvrPopupNode]].
  * Рекурсивные типы в boopickle неявно требуют интерфейса, который они будут реализовавывать.
  * Без интерфейса всё молча компилится в пустой pickler, бесконечно вызывающий сам себя.
  */
sealed trait IRcvrPopupNode {

  val nodeId          : String

  val checkbox        : Option[MRcvrPopupMeta]

  val subGroups       : Seq[MRcvrPopupGroup]

}


/** Описание узла с данными одного узла в ответе [[MRcvrPopupResp]].
  *
  * @param nodeId id узла.
  */
case class MRcvrPopupNode(
                           override val nodeId          : String,
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
  * @param name Отображаемое название узла.
  * @param isOnlineNow Сейчас активен?
  * @param dateRange Диапазон дат размещения, если есть.
  *                  Если содержит хоть какое-то значение внутри, значит уже есть проплаченное срочное размещение.
  */
case class MRcvrPopupMeta(
                           isCreate        : Boolean,
                           checked         : Boolean,
                           name            : String,
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
