package models.im.make

import scala.concurrent.{ExecutionContext, Future}

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
  def icompile(args: IMakeArgs)(implicit ec: ExecutionContext): Future[IMakeResult]

}


/** Враппер над экземпляром [[IMaker]]. */
trait IMakerWrapper extends IMaker {
  /** Заворачиваемый экземпляр [[IMaker]]. */
  def _underlying: IMaker

  override def icompile(args: IMakeArgs)(implicit ec: ExecutionContext) = _underlying.icompile(args)
}