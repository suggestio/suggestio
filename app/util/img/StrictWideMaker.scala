package util.img

import models.blk.{SzMult_t, szMulted}
import models.im.make.{IMakeResult, IMakeArgs, IMaker}
import models.im._

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 18:45
 * Description: Компилятор ссылки на широкие картинки, который НЕ квантует размеры, а генерирует строго затребованную
 * картинку на основе любой исходной.
 * Этот maker был запилен для решения ряда проблем экспорта карточек в соц.сети, у которых текст уезжал вправо за экран,
 * а слева был избыток площади из-за квантования:
 * - https://pbs.twimg.com/media/CCos2thWIAIDlm5.jpg
 * - https://pbs.twimg.com/media/CCs-V1rUMAA-01P.jpg
 *
 * Для определения размеров используются функции ImageMagic по автоматическому извлечению областей нужных размеров:
 * 1. Изображение ресайзится (обычно вниз, но бывает и вверх) до целевой ширины либо превышающей её,
 *    но совпадающей по высоте с целевой высотой: FillArea
 * 2. Gravity = center, и берётся кроп целевых размеров, упирающийся в результат (1) по высоте или ширине.
 */
object StrictWideMaker extends IMaker {

  /**
   * Собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с аргументами вызова.
   * @return Фьючерс с экземпляром [[models.im.make.IMakeResult]].
   */
  override def icompile(args: IMakeArgs)(implicit ec: ExecutionContext): Future[IMakeResult] = {
    val devScreen = args.devScreenOpt getOrElse DevScreen.default
    ???
  }

}
