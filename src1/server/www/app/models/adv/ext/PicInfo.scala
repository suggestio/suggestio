package models.adv.ext

import io.suggest.common.geom.d2.{INamedSize2di, MSize2di}
import models.blk.{OneAdWideQsArgs, SzMult_t}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 19:07
 * Description: Эта модель является контейнером для рассчитанных данных рендера
 * по картинке, которую надо отрендерить из карточки, использованной при рассчетах.
 */


/** Инфа по картинке кодируется этим классом.
  *
  * @param wide рендерить широко? С указанной шириной.
  * @param szMult Множитель масштабирования оригинала.
  * @param stdSz Штатный размер, по которому равнялись.
  */
case class PicInfo(
                    wide                  : Option[OneAdWideQsArgs],
                    wh                    : MSize2di,
                    szMult                : SzMult_t,
                    stdSz                 : INamedSize2di
                  )
