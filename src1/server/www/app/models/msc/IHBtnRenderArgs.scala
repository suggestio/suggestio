package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 17:36
 * Description: Модель аргументов для вызова рендера отдельной шаблона-кнопки в заголовке.
 * Для рендера нужен одновременный доступ к sync-контексту и к контейнеру аргументов hbtArgs.
 */
trait IHBtnRenderArgs
  extends IHBtnArgsField
  with ISyncRenderInfo
