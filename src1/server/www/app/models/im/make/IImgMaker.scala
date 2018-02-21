package models.im.make

import javax.inject.Inject

import io.suggest.model.n2.media.MMediasCache
import models.im.{MDynImgId, MImg3}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 14:27
 * Description: Интерфейс мэйкера картинок (точнее ссылок на необходимые картинки).
 */
trait IImgMaker {

  /**
   * img compile - собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с данными для вызова.
   * @return Фьючерс с экземпляром MakeResult.
   */
  def icompile(args: MImgMakeArgs): Future[MakeResult]

}
