package io.suggest.jd.render.m

import io.suggest.dev.MSzMult
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 14:59
  * Description: Модель общей конфигурации рендерера.
  */

object MJdConf {

  implicit def univEq: UnivEq[MJdConf] = UnivEq.force

}


/** Класс модели общей конфигурации рендеринга.
  *
  * @param isEdit Рендерить для редактора карточки.
  *                 Это означает, например, что некоторые элементы становятся перемещаемыми
  *                 и генерят соотв.события.
  * @param szMult Мультипликатор размера карточки.
  *               Его можно переопределить на уровне каждого конкретного блока.
  * @param oneJdGrid Использовать grid-механизмы для рендера блоков одного jd-документа?
  */
case class MJdConf(
                    isEdit              : Boolean,
                    szMult              : MSzMult,
                    oneJdGrid           : Boolean
                  ) {

  def withIsEdit(isEdit: Boolean)           = copy(isEdit = isEdit)
  def withSzMult(szMult: MSzMult)           = copy(szMult = szMult)
  def withOneJdGrid(oneJdGrid: Boolean)     = copy(oneJdGrid = oneJdGrid)

}

