package controllers

import util.PlayMacroLogsImpl
import play.api.data._, Forms._
import util.FormUtil._
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits._
import util.acl._
import views.html.sys1.market.cat._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.14 10:14
 * Description: Контроллер управления категориями магазина/ТЦ.
 */
object MarketCategory extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппинг для формы создания/редактирования пользовательской категории. */
  val catFormM = Form(mapping(
    "name"          -> nameM,
    "ymCatId"       -> nonEmptyText(minLength = 1, maxLength = 10),
    "ymCatInherit"  -> default(boolean, true),
    "position"      -> default(number, Int.MaxValue),
    "cssClass"      -> optional(nonEmptyText(minLength = 1, maxLength = 32)),
    "includeInAll"  -> default(boolean, true)
    // ownerId или parentId передаются через qs для удобства в т.ч. проверки прав доступа на редактирование категорий.
  )
  // applyF()
  {(name, ymCatId, ymCatInherit, position, cssClassOpt, includeInAll) =>
    val ymCatPtr = MMartYmCatPtr(ycId = ymCatId, inherit = ymCatInherit)
    MMartCategory(name=name, ownerId=null, parentId=null, ymCatPtr=ymCatPtr, position=position, cssClass=cssClassOpt, includeInAll=includeInAll)
  }
  // unapplyF()
  {mmcat =>
    import mmcat._
    Some((name, ymCatPtr.ycId, ymCatPtr.inherit, position, cssClass, includeInAll))
  })


  /** Отобразить страницу с деревом категорий для указанного id модели-владельца.
    * @param ownerId id магазина/тц или ещё чего-то.
    */
  def showCatsFor(ownerId: String) = TreeUserCatAdm(ownerId).async { implicit request =>
    MMartCategory.getAllTreeForOwner(ownerId) map { catTreeSeq =>
      Ok(userCatsTpl(ownerId, catTreeSeq))
    }
  }


  /**
   * Рендер страницы с формой добавления категории.
   * @param ownerId ID компонента-владельца.
   * @param parentId id родительской категории, если есть.
   */
  def addUserCatForm(ownerId: Option[String], parentId: Option[String]) = UserCatAdm(ownerId, parentId).apply { implicit request =>
    Ok(addUserCatFormTpl(request.ownerId, catFormM, parentOpt = request.cat))
  }

  /**
   * Сабмит формы создания новой модели.
   * @param ownerId id компонента-владельца.
   * @param parentId id родительской категории, если есть.
   */
  def addUserCatFormSubmit(ownerId: Option[String], parentId: Option[String]) = UserCatAdm(ownerId, parentId).async { implicit request =>
    catFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"addUserCatFormSubmit($ownerId, parentId=$parentId): Form bind failed: ${formWithErrors.errorsAsJson}")
        NotAcceptable(addUserCatFormTpl(request.ownerId, formWithErrors, parentOpt = request.cat))
      },
      {mmcat =>
        mmcat.ownerId = request.ownerId
        mmcat.parentId = parentId
        mmcat.save.map { mmcatId =>
          Redirect(routes.MarketCategory.showCatsFor(request.ownerId))
            .flashing("success" -> "Категория создана.")
        }
      }
    )
  }

  /** Отобразить данные по указанной категории. */
  def showUserCat(catId: String) = UserCatAdm(catId).async { implicit request =>
    val mmcat = request.cat.get
    val ymCatId = mmcat.ymCatPtr.ycId
    MYmCategory.getById(ymCatId) map {
      case Some(ymCat) => Ok(showUserCatTpl(mmcat, ymCat))
      case None        => catNotFound(ymCatId)
    }
  }


  /** Рендер страницы с формой редактирования указанной категории. */
  def editUserCatForm(catId: String) = UserCatAdm(catId).async { implicit request =>
    val mmcat = request.cat.get
    val parentOptFut = maybeGetParentCat(mmcat.parentId)
    parentOptFut map { parentOpt =>
      val formFilled = catFormM fill mmcat
      Ok(editUserCatFormTpl(mmcat, formFilled, parentOpt))
    }
  }

  /** Сабмит формы редактирования указанной категории. */
  def editUserCatFormSubmit(catId: String) = UserCatAdm(catId).async { implicit request =>
    val mmcat = request.cat.get
    catFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editUserCatFormSubmit($catId): Form bind failed: ${formWithErrors.errors}")
        maybeGetParentCat(mmcat.parentId) map { parentOpt =>
          NotAcceptable(editUserCatFormTpl(mmcat, formWithErrors, parentOpt))
        }
      },
      {mmcat2 =>
        mmcat.name = mmcat2.name
        mmcat.ymCatPtr = mmcat2.ymCatPtr
        mmcat.position = mmcat2.position
        mmcat.cssClass = mmcat2.cssClass
        mmcat.includeInAll = mmcat2.includeInAll
        mmcat.save.map { _ =>
          Redirect(routes.MarketCategory.showCatsFor(mmcat.ownerId))
            .flashing("success" -> "Изменения категории сохранены.")
        }
      }
    )
  }

  /** Маппер parentIdOpt на Future[Option[parentCategory] ].  */
  private def maybeGetParentCat(parentIdOpt: Option[String]): Future[Option[MMartCategory]] = {
    parentIdOpt match {
      case Some(_parentId) => MMartCategory.getById(_parentId)
      case None            => Future successful None
    }
  }

  private def catNotFound(catId: String) = NotFound("catId not found: " + catId)
}
