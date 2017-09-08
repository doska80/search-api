package com.vivareal.search.config

import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.typesafe.config.ConfigFactory

import scala.util.Try

object S3Uploader {

  private val config = ConfigFactory.load()

  private val aws = config.getConfig("aws")

  private val client = AmazonS3ClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain).withRegion(aws.getString("region")).build()

  private val acl = new AccessControlList
  acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)

  def upload(file: File, fileName: String): Unit = {
    client.putObject(new PutObjectRequest(aws.getString("s3.bucket"), s"${aws.getString("s3.folder")}/$fileName", file).withAccessControlList(acl))
  }

  private def uploadReport(sourceFolderPath: File, prefix: String = "") = {
    val path = sourceFolderPath.toPath
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult = {
        upload(file.toFile, s"$prefix${sourceFolderPath.getName}/${path.relativize(file).toString}")
        FileVisitResult.CONTINUE
      }
    })
  }

  def main(args: Array[String]): Unit = {
    val source = new File(args(0)).listFiles.sortWith((f1, f2) => f1.lastModified > f2.lastModified).head
    val prefix = Try(args(1)).map(p => s"$p/").getOrElse("")

    uploadReport(source, prefix)
    SlackNotifier.sendReportLink(source, prefix)
  }
}
