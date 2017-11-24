package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.ad.edit.m.layout.MLayoutS
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.save.MSaveS
import io.suggest.jd.MJdAdData
import io.suggest.model.n2.edge.MPredicates
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ws.pool.m.MWsPoolS
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:30
  * Description: Корневая модель состояния формы редактора рекламной карточки.
  */
object MAeRoot {

  implicit object MAeRootFastEq extends FastEq[MAeRoot] {
    override def eqv(a: MAeRoot, b: MAeRoot): Boolean = {
      (a.conf ===* b.conf) &&
        (a.doc ===* b.doc) &&
        (a.layout ===* b.layout) &&
        (a.popups ===* b.popups) &&
        (a.wsPool ===* b.wsPool) &&
        (a.save ===* b.save)
    }
  }

  implicit def univEq: UnivEq[MAeRoot] = UnivEq.derive

}


/** Класс корневой модели состояния редактора рекламной карточки.
  *
  * @param conf Конфиг, присланный сервером.
  * @param doc Состояние редактирования документа. Там почти всё и живёт.
  * @param popups Состояние попапов.
  * @param wsPool Пул коннекшенов.
  * @param layout Данные состояния layout'а формы редактирования.
  */
case class MAeRoot(
                    conf        : MAdEditFormConf,
                    doc         : MDocS,
                    layout      : MLayoutS,
                    popups      : MAePopupsS        = MAePopupsS.empty,
                    wsPool      : MWsPoolS          = MWsPoolS.empty,
                    save        : MSaveS            = MSaveS.empty
                  ) {

  /** Экспорт данных формы для отправки на сервер. */
  def toForm: MJdAdData = {
    val jdArgs = doc.jdArgs
    MJdAdData(
      template = jdArgs.template,
      edges    = {
        val videoPred = MPredicates.JdContent.Video
        jdArgs
          .edges
          .mapValues { e =>
            var jde = e.jdEdge
            if (jde.predicate !=* videoPred)
              jde = e.jdEdge.withUrl()
            jde
          }
          .values
      },
      // id карточки здесь не имеет никакого значения, т.к. он передаётся в URL.
      nodeId = None
    )
  }

  def withConf(conf: MAdEditFormConf)         = copy(conf = conf)
  def withDoc(doc: MDocS)                     = copy(doc = doc)
  def withLayout(layout: MLayoutS)            = copy(layout = layout)
  def withPopups(popups: MAePopupsS)          = copy(popups = popups)
  def withWsPool(wsPool: MWsPoolS)            = copy(wsPool = wsPool)
  def withSave(save: MSaveS)                  = copy(save = save)

}
