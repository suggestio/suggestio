package controllers

/**
 * User: Alexander Pestrikov <alexander.pestrikov@cbca.ru>
 * Date: 17.05.13
 * Time: 17:03
 * Контроллер записей "блога".
 */

import models.MBlog
import play.api.data._
import play.api.data.Forms._
import util.acl._
import views.html.blog._
import play.api.libs.concurrent.Execution.Implicits._
import Application.http404AdHoc
import util.SiowebEsUtil.client


object Blog extends SioController {

  /** Определение параметров формы для создания записи в блоге. */
  val postFormM = Form(
    mapping(
      "title"   -> nonEmptyText,
      "descr"   -> nonEmptyText,
      "bgImage" -> text,
      "bgColor" -> text,
      "text"    -> text
    )
    {(title, descr, bgImage, bgColor, text) =>
      MBlog(title=title, description=descr, bgImage=bgImage, bgColor=bgColor, text=text) }
    {rec: MBlog =>
      Some(rec.title, rec.description, rec.bgImage, rec.bgColor, rec.text) }
  )


  /**
   * Функция запроса всех записей блога
   * @return
   */

  def recordList = MaybeAuth.async { implicit request =>
    MBlog.getAll().map { blogs =>
      Ok(listTpl(blogs))
    }
  }

  /**
   * Функция запроса запис в блоге по id.
   * @param blog_id
   * @return
   */

  def readOne(blog_id: String) = MaybeAuth.async { implicit request =>
    MBlog.getById(blog_id) map {
      case Some(record) =>
        Ok(_blogRecordTpl(record, preview=false))

      case None => http404AdHoc
    }
  }


  /**
   * Функция удаление записи в блоге.
   * @param blog_id
   * @return
   */
  def deleteRecAdmin(blog_id: String) = IsSuperuser.async { implicit request =>
    // TODO пока для тестов используется maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
    MBlog.deleteById(blog_id) map {
      case true  => Redirect(routes.Blog.recordList())
      case false => NotFound("blog record not found: " + blog_id)
    }
  }



  /**
   * Сабмит формы добавления новой бложной записи в базу
   * @return
   */
  def newRecSubmit = IsSuperuser.async { implicit request =>
    // TODO пока для тестов используется maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
    postFormM.bindFromRequest.fold(
      formWithErrors =>
        NotAcceptable(addRecordTpl(formWithErrors))
      ,
      {rec =>
        rec.save map { id =>
          Redirect(routes.Blog.readOne(id))
        }
      }
    )
  }


  /**
   * Рендерим форму редактирования записи блога.
   * @param blog_id id записи блога.
   * @return 200 + Форма редакторования записи блога или 404
   */
  def editRec(blog_id: String) = IsSuperuser.async { implicit request =>
    // TODO пока для тестов используется maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
    MBlog.getById(blog_id) map {
      case Some(record) =>
        val formEdit = postFormM.fill(record)
        Ok(editFormTpl(record, formEdit))

      case None => http404AdHoc
    }
  }


  /**
   * Сабмит формы измененной бложной записи в базу.
   * @param blog_id id записи блога.
   * @return Редирект на запись или 404.
   */
  def editRecSubmit(blog_id: String) = IsSuperuser.async { implicit request =>
    // TODO пока для тестов используется maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
    MBlog.getById(blog_id) map {
      case Some(record) =>
        postFormM.bindFromRequest.fold(
          formWithErrors => NotAcceptable(editFormTpl(record, formWithErrors)),
          {record =>
            record.save
            Redirect(routes.Blog.readOne(blog_id))
          }
        )

      case None => http404AdHoc
    }
  }


  /**
   * Рендерим чистой форму для создания новой записи в блоге.
   */
  def newRec = MaybeAuth { implicit request =>
    // TODO пока для тестов используется maybeAuthenticated потом заменить на  isAdminAction (раскоментировать абзац ниже! а следующий удалить)
    Ok(addRecordTpl(postFormM))
  }

}