package com.vameli.vam.packagemanager.importer.vammodel

data class VamScene(
    val atoms: List<VamSceneAtom> = emptyList(),
)

data class VamSceneAtom(
    val id: String,
    val storables: List<VamSceneAtomStorable> = emptyList(),
)

data class VamSceneAtomStorable(
    val id: String,
    @DependencyRef val storePath: String? = null,
    @DependencyRef val presetName: String? = null,
    val clothing: List<VamSceneAtomStorableClothing> = emptyList(),
    val hair: List<VamSceneAtomStorableHair> = emptyList(),
    val clips: List<VamSceneAtomStorableClip> = emptyList(),
    val morphs: List<VamSceneAtomStorableMorph> = emptyList(),
)

data class VamSceneAtomStorableClothing(
    @DependencyRef val id: String,
)

data class VamSceneAtomStorableHair(
    @DependencyRef val id: String,
)

data class VamSceneAtomStorableClip(
    @DependencyRef val url: String,
)

data class VamSceneAtomStorableMorph(
    @DependencyRef val uid: String,
)
