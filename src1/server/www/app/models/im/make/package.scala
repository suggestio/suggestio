package models.im

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 12:30
 * im.make - это системы создавания изображений. Они, на основе исходных данных об изображении и карточки, создают
 * набор инструкций для imagemagic, чтобы та произвела необходимое изображение из исходного.
 *
 * Конкретным продуктом работы make'ов являются ссылки на изображения и данные о производных изображениях.
 * Именно эти данные возвращаются юзеру через HTML и иные каналы.
 */
package object make {

  type Maker      = Makers.T

  /** Тип для маппинга системной формы. Передается из контроллера в шаблоны. */
  type SysForm_t  = Form[(Maker, IMakeArgs)]

}
