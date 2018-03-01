package models.im

import io.suggest.common.geom.d2.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.11.15 22:07
 * Description: Модель исчерпывающей инфы по картинке, которую можно отрендерить в шаблоне ссылку.
 */

final case class MImgWithWhInfo(
                                 mimg : MImgT,
                                 meta : ISize2di
                               )

