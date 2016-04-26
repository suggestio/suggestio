package models.im.make

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 14:27
 * Description: Интерфейс мэйкера картинок (точнее ссылок на необходимые картинки).
 */
trait IMaker {

  /**
   * img compile - собрать ссылку на изображение и сопутствующие метаданные.
   * @param args Контейнер с данными для вызова.
   * @return Фьючерс с экземпляром MakeResult.
   */
  def icompile(args: IMakeArgs): Future[MakeResult]

}
