package dev.melodify.uranophilelab.model.aboutus


data class Contributors(
    val contributors: MutableList<Contributor?>?
) {
    
    data class Contributor(
        val login: String?,
        val avatar_url: String?,
        val html_url: String?
    )
}
