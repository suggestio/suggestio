package io.suggest.sc.m

import boopickle.Default._
import io.suggest.model.n2.node.meta.colors.MColors

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 18:31
  * Description: Кросс-платформенная модель данных по отображаемому узлу для нужд выдачи.
  * Под узлом тут в первую очередь подразумевается узел выдачи, а не конкретная узел-карточка.
  */
object MScNodeInfo {

  /** Поддержка бинарной сериализации. */
  implicit val mScNodeProdInfoPickler: Pickler[MScNodeInfo] = {
    implicit val mColorsP = MColors.mColorsPickler
    generatePickler[MScNodeInfo]
  }

}

/** Контейнер данных по узлу в интересах выдачи.
  * На всякий случай, модель максимально толерантна к данными и целиком необязательна.
  *
  * @param nodeId id узла в s.io.
  * @param name Название узла, если есть.
  * @param colors Цвета, если есть.
  * @param logoOpt Данные по логотипу-иллюстрации.
  */
case class MScNodeInfo(
                        nodeId  : Option[String],
                        name    : Option[String],
                        colors  : MColors,
                        logoOpt : Option[MScNodeLogoInfo]
                      )


/** Информация по логотипу узла. */
case class MScNodeLogoInfo(
                            url: String
                          )
