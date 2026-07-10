package com.example.mangaapp.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mangaapp.models.Comment

class CommentDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "manga_app.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_COMMENTS = "comments"
        private const val COL_ID = "id"
        private const val COL_FIRESTORE_ID = "firestore_id"
        private const val COL_CHAPTER_ID = "chapter_id"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "user_name"
        private const val COL_AVATAR_URL = "avatar_url"
        private const val COL_CONTENT = "content"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_LIKES = "likes"

        @Volatile
        private var INSTANCE: CommentDbHelper? = null

        fun getInstance(context: Context): CommentDbHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommentDbHelper(context).also { INSTANCE = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_COMMENTS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_FIRESTORE_ID TEXT NOT NULL,
                $COL_CHAPTER_ID INTEGER,
                $COL_USER_ID TEXT,
                $COL_USER_NAME TEXT,
                $COL_AVATAR_URL TEXT,
                $COL_CONTENT TEXT,
                $COL_TIMESTAMP INTEGER,
                $COL_LIKES INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_comments_story ON $TABLE_COMMENTS($COL_FIRESTORE_ID, $COL_CHAPTER_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        onCreate(db)
    }

    /** Thêm (hoặc thay thế nếu trùng id) một bình luận */
    fun insertComment(comment: Comment) {
        val values = ContentValues().apply {
            put(COL_ID, comment.id)
            put(COL_FIRESTORE_ID, comment.firestoreId)
            if (comment.chapterId != null) put(COL_CHAPTER_ID, comment.chapterId) else putNull(COL_CHAPTER_ID)
            put(COL_USER_ID, comment.userId)
            put(COL_USER_NAME, comment.userName)
            put(COL_AVATAR_URL, comment.avatarUrl)
            put(COL_CONTENT, comment.content)
            put(COL_TIMESTAMP, comment.timestamp)
            put(COL_LIKES, comment.likes)
        }
        writableDatabase.insertWithOnConflict(TABLE_COMMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Lấy toàn bộ bình luận đang lưu (dùng khi khởi tạo cache trong bộ nhớ) */
    fun getAllComments(): MutableList<Comment> {
        val list = mutableListOf<Comment>()
        readableDatabase.query(TABLE_COMMENTS, null, null, null, null, null, "$COL_TIMESTAMP ASC")
            .use { cursor -> while (cursor.moveToNext()) list.add(cursorToComment(cursor)) }
        return list
    }

    /** Bình luận của cả bộ truyện (không gắn với chapter cụ thể) */
    fun getCommentsByStory(firestoreId: String): List<Comment> {
        val list = mutableListOf<Comment>()
        readableDatabase.query(
            TABLE_COMMENTS, null,
            "$COL_FIRESTORE_ID = ? AND $COL_CHAPTER_ID IS NULL",
            arrayOf(firestoreId), null, null, "$COL_TIMESTAMP ASC"
        ).use { cursor -> while (cursor.moveToNext()) list.add(cursorToComment(cursor)) }
        return list
    }

    /** Bình luận theo từng chapter cụ thể */
    fun getCommentsByChapter(firestoreId: String, chapterId: Int): List<Comment> {
        val list = mutableListOf<Comment>()
        readableDatabase.query(
            TABLE_COMMENTS, null,
            "$COL_FIRESTORE_ID = ? AND $COL_CHAPTER_ID = ?",
            arrayOf(firestoreId, chapterId.toString()), null, null, "$COL_TIMESTAMP ASC"
        ).use { cursor -> while (cursor.moveToNext()) list.add(cursorToComment(cursor)) }
        return list
    }

    /** Xoá toàn bộ bình luận đã lưu cục bộ (dùng khi cần reset) */
    fun clearAll() {
        writableDatabase.delete(TABLE_COMMENTS, null, null)
    }

    private fun cursorToComment(cursor: Cursor): Comment {
        val chapterIdIndex = cursor.getColumnIndexOrThrow(COL_CHAPTER_ID)
        return Comment(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
            firestoreId = cursor.getString(cursor.getColumnIndexOrThrow(COL_FIRESTORE_ID)),
            chapterId = if (cursor.isNull(chapterIdIndex)) null else cursor.getInt(chapterIdIndex),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)) ?: "",
            userName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)) ?: "",
            avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_AVATAR_URL)) ?: "",
            content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)) ?: "",
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            likes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LIKES))
        )
    }
}