package com.darkrockstudios.apps.hammer.common.fileio.okio

import com.darkrockstudios.apps.hammer.common.data.*
import com.darkrockstudios.apps.hammer.common.fileio.HPath
import com.darkrockstudios.apps.hammer.common.tree.ImmutableTree
import com.darkrockstudios.apps.hammer.common.tree.TreeNode
import com.darkrockstudios.apps.hammer.common.util.numDigits
import io.github.aakira.napier.Napier
import okio.FileSystem
import okio.IOException
import okio.Path

class ProjectEditorRepositoryOkio(
    projectDef: ProjectDef,
    projectsRepository: ProjectsRepository,
    private val fileSystem: FileSystem
) : ProjectEditorRepository(projectDef, projectsRepository) {

    override fun getSceneFilename(path: HPath) = path.toOkioPath().name

    override fun getSceneParentPath(path: HPath): ScenePathSegments {
        val parentPath = path.toOkioPath().parent
        return if (parentPath != null && parentPath.name != "..") {
            getScenePathSegments(parentPath.toHPath())
        } else {
            ScenePathSegments(emptyList())
        }
    }

    override fun getScenePathSegments(path: HPath): ScenePathSegments {
        val parentPath = path.toOkioPath()

        val sceneDir = getSceneDirectory().toOkioPath()
        return if (parentPath != sceneDir) {
            val sceneId = getSceneIdFromFilename(path)
            val parentScenes = sceneTree.getBranch(true) { it.id == sceneId }
                .map { it.value.id }
            ScenePathSegments(pathSegments = parentScenes)
        } else {
            ScenePathSegments(pathSegments = emptyList())
        }
    }

    override fun getHpath(sceneItem: SceneItem): HPath {
        val sceneDir = getSceneDirectory().toOkioPath()

        val sceneNode = sceneTree.find { it.id == sceneItem.id }
        val pathSegments = sceneTree.getBranch(sceneNode, true)
            .map { node -> node.value }
            .map { item ->
                val itemPath = getSceneFilePath(item)
                getSceneFilename(itemPath)
            }

        var path = sceneDir
        pathSegments.forEach { filename ->
            path = path.div(filename)
        }

        return path.toHPath()
    }

    override fun getSceneDirectory(): HPath {
        val projOkPath = projectDef.path.toOkioPath()
        val sceneDirPath = projOkPath.div(SCENE_DIRECTORY)
        if (!fileSystem.exists(sceneDirPath)) {
            fileSystem.createDirectory(sceneDirPath)
        }
        return sceneDirPath.toHPath()
    }

    override fun getSceneBufferDirectory(): HPath {
        val projOkPath = projectDef.path.toOkioPath()
        val sceneDirPath = projOkPath.div(SCENE_DIRECTORY)
        val bufferPathSegment = sceneDirPath.div(BUFFER_DIRECTORY)
        if (!fileSystem.exists(bufferPathSegment)) {
            fileSystem.createDirectory(bufferPathSegment)
        }
        return bufferPathSegment.toHPath()
    }

    override fun getSceneFilePath(scene: SceneItem, isNewScene: Boolean): HPath {
        val scenePathSegment = getSceneDirectory().toOkioPath()

        val pathSegments = sceneTree.getBranch(true) { it.id == scene.id }
            .map { node -> node.value }
            .filter { scene -> !scene.isRootScene }
            .map { scene -> getSceneFileName(scene) }
            .toMutableList()

        pathSegments.add(getSceneFileName(scene, isNewScene))

        var fullPath: Path = scenePathSegment
        pathSegments.forEach { segment ->
            fullPath = fullPath.div(segment)
        }

        return fullPath.toHPath()
    }

    override fun getSceneFilePath(sceneId: Int): HPath {
        val scenePathSegment = getSceneDirectory().toOkioPath()

        val branch = sceneTree.getBranch { it.id == sceneId }
        val pathSegments = branch
            .map { node -> node.value }
            .filter { sceneItem -> !sceneItem.isRootScene }
            .map { sceneItem -> getSceneFileName(sceneItem) }

        var fullPath: Path = scenePathSegment
        pathSegments.forEach { segment ->
            fullPath = fullPath.div(segment)
        }

        return fullPath.toHPath()
    }

    override fun getSceneBufferTempPath(sceneDef: SceneItem): HPath {
        val bufferPathSegment = getSceneBufferDirectory().toOkioPath()
        val fileName = getSceneTempFileName(sceneDef)
        return bufferPathSegment.div(fileName).toHPath()
    }

    override fun getSceneFromPath(path: HPath): SceneItem {
        val sceneDef = getSceneFromFilename(path)
        return sceneDef
    }

    override fun loadSceneTree(): TreeNode<SceneItem> {
        val sceneDirPath = getSceneDirectory().toOkioPath()
        val rootNode = TreeNode(rootScene)

        val childNodes = fileSystem.list(sceneDirPath)
            .filter { it.name != BUFFER_DIRECTORY }
            .filter { !it.name.endsWith(tempSuffix) }
            .sortedBy { it.name }
            .map { path -> loadSceneTreeNode(path) }

        for (child in childNodes) {
            rootNode.addChild(child)
        }

        return rootNode
    }

    private fun loadSceneTreeNode(root: Path): TreeNode<SceneItem> {
        val scene = getSceneFromPath(root.toHPath())
        val node = TreeNode(scene)

        if (fileSystem.metadata(root).isDirectory) {
            val childNodes = fileSystem.list(root)
                .filter { it.name != BUFFER_DIRECTORY }
                .filter { !it.name.endsWith(tempSuffix) }
                .sortedBy { it.name }
                .map { path -> loadSceneTreeNode(path) }

            for (child in childNodes) {
                node.addChild(child)
            }
        }

        return node
    }

    private fun getAllScenePathsOkio(): List<Path> {
        val sceneDirPath = getSceneDirectory().toOkioPath()
        val scenePaths = fileSystem.listRecursively(sceneDirPath)
            .filter { it.name != BUFFER_DIRECTORY }
            .filter { !it.name.endsWith(tempSuffix) }
            .toList()
            .sortedBy { it.name }
        return scenePaths
    }

    private fun getScenePathsOkio(root: Path): List<Path> {
        val scenePaths = fileSystem.list(root)
            .filter { it.name != BUFFER_DIRECTORY }
            .filter { !it.name.endsWith(tempSuffix) }
            .sortedBy { it.name }
        return scenePaths
    }

    private fun getGroupChildPathsById(root: Path): Map<Int, Path> {
        return getScenePathsOkio(root)
            .map { scenePath ->
                val sceneId = getSceneIdFromFilename(scenePath.toHPath())
                Pair(sceneId, scenePath)
            }.associateBy({ it.first }, { it.second })
    }

    fun getIndex(node: TreeNode<SceneItem>): Int {
        return sceneTree.indexOf(node)
    }

    fun getIndex(sceneId: Int): Int {
        return sceneTree.indexOfFirst { it.value.id == sceneId }
    }

    private fun updateSceneTreeForMove(moveRequest: MoveRequest) {
        val fromNode = sceneTree.find { it.id == moveRequest.id }
        val toParentNode = sceneTree[moveRequest.toPosition.coords.parentIndex]
        val insertIndex = moveRequest.toPosition.coords.childLocalIndex

        Napier.d("Move Scene Item: $moveRequest")

        val fromParent = fromNode.parent
        val fromIndex = fromParent?.localIndexOf(fromNode) ?: -1
        val changingParents = (toParentNode != fromParent)

        val finalIndex = if (toParentNode.numChildrenImmedate() == 0) {
            0
        } else {
            if (!changingParents) {
                if (fromIndex <= insertIndex) {
                    if (moveRequest.toPosition.before) {
                        (insertIndex - 1).coerceAtLeast(0)
                    } else {
                        insertIndex
                    }
                } else {
                    if (moveRequest.toPosition.before) {
                        insertIndex
                    } else {
                        insertIndex + 1
                    }
                }
            } else {
                if (moveRequest.toPosition.before) {
                    insertIndex
                } else {
                    insertIndex + 1
                }
            }
        }

        toParentNode.insertChild(finalIndex, fromNode)

        /*
        // Move debugging
        println("Before Move:")
        sceneTree.print()

        println("After Move:")
        sceneTree.print()
        */
    }

    override fun moveScene(moveRequest: MoveRequest) {
        val fromNode = sceneTree.find { it.id == moveRequest.id }
        val fromParentNode = fromNode.parent
            ?: throw IllegalStateException("Item had no parent")
        val toParentNode = sceneTree[moveRequest.toPosition.coords.parentIndex]

        val isMovingParents = (fromParentNode != toParentNode)

        // Perform move inside tree
        updateSceneTreeForMove(moveRequest)

        // Moving from one parent to another
        if (isMovingParents) {
            // Move the file to it's new parent
            val toPath = getSceneFilePath(moveRequest.id)

            val fromParentPath = getSceneFilePath(fromParentNode.value.id)
            val originalFromParentScenePaths = getGroupChildPathsById(fromParentPath.toOkioPath())
            val originalFromNodePath = originalFromParentScenePaths[fromNode.value.id]
                ?: throw IllegalStateException("From node wasn't where it's supposed to be")

            fileSystem.atomicMove(
                source = originalFromNodePath,
                target = toPath.toOkioPath()
            )

            // Update new parents children
            updateSceneOrder(toParentNode.value.id)

            // Update original parents children
            updateSceneOrder(fromParentNode.value.id)
        }
        // Moving inside same parent
        else {
            updateSceneOrder(toParentNode.value.id)
        }

        // Notify listeners of the new state of the tree
        val imTree = sceneTree.toImmutableTree()

        val newSummary = SceneSummary(
            imTree,
            getDirtyBufferIds()
        )
        reloadScenes(newSummary)
    }

    override fun updateSceneOrder(parentId: Int) {
        val parent = sceneTree.find { it.id == parentId }
        if (parent.value.type == SceneItem.Type.Scene) throw IllegalArgumentException("SceneItem must be Root or Group")

        val parentPath = getSceneFilePath(parent.value.id)
        val existingSceneFiles = getGroupChildPathsById(parentPath.toOkioPath())

        parent.children().forEachIndexed { index, childNode ->
            childNode.value = childNode.value.copy(order = index)

            val existingPath = existingSceneFiles[childNode.value.id]
                ?: throw IllegalStateException("Scene wasn't present in directory")
            val newPath = getSceneFilePath(childNode.value.id).toOkioPath()

            if (existingPath != newPath) {
                try {
                    fileSystem.atomicMove(source = existingPath, target = newPath)
                } catch (e: IOException) {
                    throw IOException("existingPath: $existingPath\nnewPath: $newPath\n${e.message}")
                }
            }
        }
    }

    override fun createScene(parent: SceneItem?, sceneName: String): SceneItem? {
        return createSceneItem(parent, sceneName, false)
    }

    override fun createGroup(parent: SceneItem?, groupName: String): SceneItem? {
        return createSceneItem(parent, groupName, true)
    }

    private fun createSceneItem(parent: SceneItem?, name: String, isGroup: Boolean): SceneItem? {
        val cleanedNamed = name.trim()

        return if (!validateSceneName(cleanedNamed)) {
            Napier.d("Invalid scene name")
            null
        } else {
            val lastOrder = getLastOrderNumber(parent?.id)
            val nextOrder = lastOrder + 1
            val sceneId = claimNextSceneId()
            val type = if (isGroup) SceneItem.Type.Group else SceneItem.Type.Scene

            val newSceneItem = SceneItem(
                projectDef = projectDef,
                type = type,
                id = sceneId,
                name = cleanedNamed,
                order = nextOrder,
            )

            val newTreeNode = TreeNode(newSceneItem)
            if (parent != null) {
                val parentNode = sceneTree.find { it.id == parent.id }
                parentNode.addChild(newTreeNode)
            } else {
                sceneTree.addChild(newTreeNode)
            }

            val scenePath = getSceneFilePath(newSceneItem, true).toOkioPath()
            when (type) {
                SceneItem.Type.Scene -> fileSystem.write(scenePath, true) {
                    writeUtf8("")
                }
                SceneItem.Type.Group -> fileSystem.createDirectory(scenePath, true)
                SceneItem.Type.Root -> throw IllegalArgumentException("Cannot create Root")
            }

            // If we need to increase the padding digits, update the file names
            if (lastOrder.numDigits() < nextOrder.numDigits()) {
                updateSceneOrder(parent?.id ?: SceneItem.ROOT_ID)
            }

            Napier.i("createScene: $cleanedNamed")

            reloadScenes()

            newSceneItem
        }
    }

    override fun deleteScene(scene: SceneItem): Boolean {
        val scenePath = getSceneFilePath(scene).toOkioPath()
        return try {
            if (!fileSystem.exists(scenePath)) {
                Napier.e("Tried to delete Scene, but file did not exist")
                false
            } else if (!fileSystem.metadata(scenePath).isRegularFile) {
                Napier.e("Tried to delete Scene, but file was not File")
                false
            } else {
                fileSystem.delete(scenePath)

                val sceneNode = getSceneNodeFromId(scene.id)

                val parent = sceneNode?.parent
                if (parent != null) {
                    val parentId: Int = parent.value.id
                    parent.removeChild(sceneNode)

                    updateSceneOrder(parentId)
                    Napier.w("Scene ${scene.id} deleted")

                    reloadScenes()

                    true
                } else {
                    Napier.w("Failed to delete scene ${scene.id}")
                    false
                }
            }
        } catch (e: IOException) {
            Napier.e("Failed to delete Group ID ${scene.id}: ${e.message}")
            false
        }
    }

    override fun deleteGroup(scene: SceneItem): Boolean {
        val scenePath = getSceneFilePath(scene).toOkioPath()
        return try {
            if (!fileSystem.exists(scenePath)) {
                Napier.e("Tried to delete Group, but file did not exist")
                false
            } else if (!fileSystem.metadata(scenePath).isDirectory) {
                Napier.e("Tried to delete Group, but file was not Directory")
                false
            } else if (fileSystem.list(scenePath).isNotEmpty()) {
                Napier.w("Tried to delete Group, but was not empty")
                false
            } else {
                fileSystem.delete(scenePath)

                val sceneNode = getSceneNodeFromId(scene.id)

                val parent = sceneNode?.parent
                if (parent != null) {
                    val parentId: Int = parent.value.id
                    parent.removeChild(sceneNode)

                    updateSceneOrder(parentId)
                    Napier.w("Group ${scene.id} deleted")

                    reloadScenes()

                    true
                } else {
                    Napier.w("Failed to delete group ${scene.id}")
                    false
                }
            }
        } catch (e: IOException) {
            Napier.e("Failed to delete Group ID ${scene.id}: ${e.message}")
            false
        }
    }

    override fun getScenes(): List<SceneItem> {
        return getAllScenePathsOkio()
            .filter { it.name != BUFFER_DIRECTORY }
            .filter { !it.name.endsWith(tempSuffix) && it.name != ".." }
            .map { path ->
                getSceneFromFilename(path.toHPath())
            }
    }

    override fun getSceneTree(): ImmutableTree<SceneItem> {
        return sceneTree.toImmutableTree()
    }

    override fun getScenes(root: HPath): List<SceneItem> {
        val rootOkia = root.toOkioPath()
        return getScenePathsOkio(rootOkia)
            .filter { it.name != BUFFER_DIRECTORY }
            .filter { !it.name.endsWith(tempSuffix) && it.name != ".." }
            .map { path ->
                getSceneFromFilename(path.toHPath())
            }
    }

    override fun getSceneTempBufferContents(): List<SceneContent> {
        val bufferDirectory = getSceneBufferDirectory().toOkioPath()
        return fileSystem.list(bufferDirectory)
            .filter { fileSystem.metadata(it).isRegularFile }
            .mapNotNull { path ->
                val id = getSceneIdFromBufferFilename(path.name)
                getSceneItemFromId(id)
            }
            .map { sceneDef ->
                val tempPath = getSceneBufferTempPath(sceneDef).toOkioPath()
                val content = try {
                    fileSystem.read(tempPath) {
                        readUtf8()
                    }
                } catch (e: IOException) {
                    Napier.e("Failed to load Scene (${sceneDef.name})")
                    ""
                }
                SceneContent(sceneDef, content)
            }
    }

    override fun getSceneAtIndex(index: Int): SceneItem {
        val scenePaths = getAllScenePathsOkio()
        if (index >= scenePaths.size) throw IllegalArgumentException("Invalid scene index requested: $index")
        val scenePath = scenePaths[index]
        return getSceneFromPath(scenePath.toHPath())
    }

    override fun loadSceneBuffer(sceneDef: SceneItem): SceneBuffer {
        val scenePath = getSceneFilePath(sceneDef).toOkioPath()

        return if (hasSceneBuffer(sceneDef)) {
            getSceneBuffer(sceneDef)
                ?: throw IllegalStateException("sceneBuffers did not contain buffer for scene: ${sceneDef.id} - ${sceneDef.name}")
        } else {
            val content = try {
                fileSystem.read(scenePath) {
                    readUtf8()
                }
            } catch (e: IOException) {
                Napier.e("Failed to load Scene (${sceneDef.name})")
                ""
            }

            val newBuffer = SceneBuffer(
                SceneContent(sceneDef, content)
            )

            updateSceneBuffer(newBuffer)

            newBuffer
        }
    }

    override fun storeSceneBuffer(sceneDef: SceneItem): Boolean {
        val buffer = getSceneBuffer(sceneDef)
        if (buffer == null) {
            Napier.e { "Failed to store scene: ${sceneDef.id} - ${sceneDef.name}, no buffer present" }
            return false
        }

        val scenePath = getSceneFilePath(sceneDef).toOkioPath()

        return try {
            val markdown = buffer.content.coerceMarkdown()

            fileSystem.write(scenePath) {
                writeUtf8(markdown)
            }

            val cleanBuffer = buffer.copy(dirty = false)
            updateSceneBuffer(cleanBuffer)

            cancelTempStoreJob(sceneDef)
            clearTempScene(sceneDef)

            true
        } catch (e: IOException) {
            Napier.e("Failed to store scene: (${sceneDef.name}) with error: ${e.message}")
            false
        }
    }

    override fun storeTempSceneBuffer(sceneDef: SceneItem): Boolean {
        val buffer = getSceneBuffer(sceneDef)
        if (buffer == null) {
            Napier.e { "Failed to store scene: ${sceneDef.id} - ${sceneDef.name}, no buffer present" }
            return false
        }

        val scenePath = getSceneBufferTempPath(sceneDef).toOkioPath()

        return try {
            val markdown = buffer.content.coerceMarkdown()

            fileSystem.write(scenePath) {
                writeUtf8(markdown)
            }

            Napier.e("Stored temp scene: (${sceneDef.name})")

            true
        } catch (e: IOException) {
            Napier.e("Failed to store temp scene: (${sceneDef.name}) with error: ${e.message}")
            false
        }
    }

    override fun clearTempScene(sceneDef: SceneItem) {
        val path = getSceneBufferTempPath(sceneDef).toOkioPath()
        fileSystem.delete(path)
    }

    override fun getLastOrderNumber(parentPath: HPath): Int {
        val numScenes = fileSystem.list(parentPath.toOkioPath()).count {
            fileSystem.metadataOrNull(it)?.isRegularFile == true
        }
        return numScenes
    }

    override fun getLastOrderNumber(parentId: Int?): Int {
        val parentPath: HPath = if (parentId != null && parentId != 0) {
            val parentItem =
                getSceneItemFromId(parentId) ?: throw IllegalStateException("Parent not found")

            getSceneFilePath(parentItem)
        } else {
            getSceneDirectory()
        }

        val numScenes = fileSystem.list(parentPath.toOkioPath())
            .count { it.name != BUFFER_DIRECTORY } - 1
        return numScenes
    }

    override fun renameScene(sceneItem: SceneItem, newName: String) {
        val cleanedNamed = newName.trim()

        val oldPath = getSceneFilePath(sceneItem).toOkioPath()
        val newDef = sceneItem.copy(name = cleanedNamed)

        val node = getSceneNodeFromId(sceneItem.id)
            ?: throw IllegalStateException("Failed to get scene for renaming: ${sceneItem.id}")
        node.value = newDef

        val newPath = getSceneFilePath(newDef).toOkioPath()

        fileSystem.atomicMove(oldPath, newPath)

        reloadScenes()
    }
}