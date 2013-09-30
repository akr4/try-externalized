package test

import java.io._

import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._
import unfiltered.netty.request._
import unfiltered.netty.cycle.Plan

import com.github.cb372.util.process._
import com.github.cb372.util.process.StreamProcessing.consume

import org.apache.commons.io.IOUtils

object PictureApi extends cycle.Plan with cycle.ThreadPool with ServerErrorResponse {
  def intent = {
    case GET(Path(Seg("pictures" :: "form" :: Nil))) =>
      Ok ~> Html(
        <form action="/pictures" method="post" enctype="multipart/form-data">
          <input type="file" name="picture"/>
          <input type="submit" value="upload"/>
        </form>
      )

    case POST(Path(Seg("pictures" :: Nil))) & MultiPart(multipart) => {
      val part = MultiPartParams.Disk(multipart)
      part.files("picture").headOption match {
        case Some(picture) =>
          saveOriginalPicture(picture)
          saveSmallPicture(picture)
          Redirect("/pictures/form")
        case _ =>
          BadRequest
      }
    }
  }

  private def saveOriginalPicture(picture: AbstractDiskFile) {
    val out = new FileOutputStream(new File("original.jpg"))
    out.write(picture.bytes)
  }

  private def saveSmallPicture(picture: AbstractDiskFile) {
    val process = Command.parse("""convert - -auto-orient -strip -quality 80 -resize 300x300> -gaussian-blur 0.05 jpg:-""").
      processStdOut(consume().asBinary()).
      collectStdOut().
      start()

    IOUtils.copy(new ByteArrayInputStream(picture.bytes), process.getStdIn())
    process.getStdIn().close()
    process.waitFor()

    val out = new FileOutputStream(new File("small.jpg"))
    out.write(process.getBinaryOutput())
  }
}

