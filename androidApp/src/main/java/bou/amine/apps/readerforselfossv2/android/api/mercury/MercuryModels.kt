package bou.amine.apps.readerforselfossv2.android.api.mercury

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

class ParsedContent(
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String?,
    @SerializedName("date_published") val date_published: String,
    @SerializedName("lead_image_url") val lead_image_url: String?,
    @SerializedName("dek") val dek: String,
    @SerializedName("url") val url: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("excerpt") val excerpt: String,
    @SerializedName("total_pages") val total_pages: Int,
    @SerializedName("rendered_pages") val rendered_pages: Int,
    @SerializedName("next_page_url") val next_page_url: String
) : Parcelable {

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParsedContent> =
            object : Parcelable.Creator<ParsedContent> {
                override fun createFromParcel(source: Parcel): ParsedContent = ParsedContent(source)
                override fun newArray(size: Int): Array<ParsedContent?> = arrayOfNulls(size)
            }
    }

    constructor(source: Parcel) : this(
        title = source.readString().orEmpty(),
        content = source.readString(),
        date_published = source.readString().orEmpty(),
        lead_image_url = source.readString(),
        dek = source.readString().orEmpty(),
        url = source.readString().orEmpty(),
        domain = source.readString().orEmpty(),
        excerpt = source.readString().orEmpty(),
        total_pages = source.readInt(),
        rendered_pages = source.readInt(),
        next_page_url = source.readString().orEmpty()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(content)
        dest.writeString(date_published)
        dest.writeString(lead_image_url)
        dest.writeString(dek)
        dest.writeString(url)
        dest.writeString(domain)
        dest.writeString(excerpt)
        dest.writeInt(total_pages)
        dest.writeInt(rendered_pages)
        dest.writeString(next_page_url)
    }
}
