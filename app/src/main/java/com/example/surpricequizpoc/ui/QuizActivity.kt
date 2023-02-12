package com.example.surpricequizpoc.ui

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.surpricequizpoc.R
import com.example.surpricequizpoc.adapter.setAnswerKeyAdapter
import com.example.surpricequizpoc.adapter.QuestionAdapter
import com.example.surpricequizpoc.databinding.ActivityMainBinding

import com.example.surpricequizpoc.model.Questions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar


import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class QuizActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val quizViewModel: QuizViewModel by viewModel()

    private var questionAdapter: QuestionAdapter? = null

    private var setAnswerKeyAdapter: setAnswerKeyAdapter? = null

    private var questionPosition:Int?=null

    private var optionPosition:Int?=null

    var imageUri: Uri? = null

//    private val requestMultiplePermissions by lazy {
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions->
//            permissions.entries.forEach {
//                var permissionName = it.key
//                val isGranted = it.value
//                if(isGranted){
//                    //isGranted
//                }else{
//
//                }
//            }
//        }
//    }
    private val requestPermission by lazy{
    registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted->
        if(isGranted){
            //permission Granted
            Toast.makeText(this,"permission Granted...",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this,"permission Denied...",Toast.LENGTH_SHORT).show()
        }

    }
}

    private val requestTopTakePictureForQuestion by lazy{
        registerForActivityResult(ActivityResultContracts.TakePicture()){
            questionPosition?.let { it1 -> quizViewModel.addQuestionImage(it1, questionImage = imageUri) }
        }
    }

    private val requestTopTakePictureForOption by lazy{
        registerForActivityResult(ActivityResultContracts.TakePicture()){
            questionPosition?.let { questionPosition -> optionPosition?.let { optionPosition ->
                quizViewModel.addOptionImage(questionPosition,
                    optionPosition,imageUri)
            } }
        }
    }
    private val requestToGetQuestionImageFromFile by lazy {
        registerForActivityResult(ActivityResultContracts.GetContent()) { questionImageFromGallery ->
            questionPosition?.let { quizViewModel.addQuestionImage(it, questionImage = questionImageFromGallery) }
        }
    }

    private val requestToGetOptionImageFromFile by lazy {
        registerForActivityResult(ActivityResultContracts.GetContent()) { optionImageFromGallery ->
            optionPosition?.let { questionPosition?.let { questionPosition -> quizViewModel.addOptionImage(questionPosition, it, optionImage = optionImageFromGallery) } }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        requestPermission.launch(CAMERA)
        requestTopTakePictureForQuestion
        requestTopTakePictureForOption
        requestToGetQuestionImageFromFile
        requestToGetOptionImageFromFile

        imageUri = initTempUri()
        setUpObserver()
        setUpListener()

    }

    private fun initTempUri(): Uri {
        val tempImagesDir = File(applicationContext.filesDir,getString(R.string.temp_images_dir))
        tempImagesDir.mkdir()
        val tempImage = File(tempImagesDir,getString(R.string.temp_image))
        return FileProvider.getUriForFile(applicationContext,getString(R.string.authorities),tempImage)
    }

    private fun setUpObserver() {
        quizViewModel.quizDataList.observe(this) { questionList ->
            setUpAdapter(questionList)
        }
    }

    private fun setUpAdapter(questionList: List<Questions>?) {
        questionAdapter = QuestionAdapter(
            questionList = questionList?.toMutableList() ?: mutableListOf(),
            addNewOption = ::addNewOption,
            deleteOption = ::removeOption,
            deleteQuestion = ::removeQuestion,
            questionTitleChange = ::onQuestionTitleChange,
            optionTitleChange = ::onOptionTextChange,
            addAnotherQuestion = ::addQuestion,
            copyQuestion = ::copyQuestion,
            onOptionSelected = ::onOptionSelected,
            setAnswerKey = ::setAnswerKey,
            getQuestionImage = { questionAdapterPosition ->
                val view = layoutInflater.inflate(R.layout.select_picture_bottom_sheet, null)
                val btnClose = view.findViewById<ImageView>(R.id.uiIvBottomCloseClose)
                val dialog = BottomSheetDialog(this)
                val uiGetImageFromCamera = view.findViewById<ImageView>(R.id.uiIvSelectImageFromCameraApp)
                val uiGetImageFromGallery = view.findViewById<ImageView>(R.id.uiIvSelectImageFromGallery)

                uiGetImageFromCamera.setOnClickListener {
                    requestTopTakePictureForQuestion.launch(imageUri)
                    questionPosition = questionAdapterPosition
                    dialog.dismiss()
                }
                uiGetImageFromGallery.setOnClickListener {
                    requestToGetQuestionImageFromFile.launch("image/*")
                    questionPosition = questionAdapterPosition
                    dialog.dismiss()
                }
                btnClose.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setCancelable(false)
                dialog.setContentView(view)
                dialog.show()
            },
            getOptionImage = { questionAdapterPosition, optionAdapterPosition ->
                val view = layoutInflater.inflate(R.layout.select_picture_bottom_sheet, null)
                val btnClose = view.findViewById<ImageView>(R.id.uiIvBottomCloseClose)
                val dialog = BottomSheetDialog(this)
                val uiGetImageFromOpenCamera = view.findViewById<ImageView>(R.id.uiIvSelectImageFromCameraApp)
                val uiGetImageFromGallery = view.findViewById<ImageView>(R.id.uiIvSelectImageFromGallery)

                uiGetImageFromOpenCamera.setOnClickListener {
                    requestTopTakePictureForOption.launch(imageUri)
                    questionPosition = questionAdapterPosition
                    optionPosition = optionAdapterPosition

                    dialog.dismiss()
                }
                uiGetImageFromGallery.setOnClickListener {
                    requestToGetOptionImageFromFile.launch("image/*")
                    questionPosition = questionAdapterPosition
                    optionPosition = optionAdapterPosition

                    dialog.dismiss()
                }
                btnClose.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setCancelable(false)
                dialog.setContentView(view)
                dialog.show()

            }
        )
        binding.uiRvQuestion.adapter = questionAdapter
        (binding.uiRvQuestion.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    private fun setUpListener() {
        binding.uiBtAddQuestions.setOnClickListener {
            Log.e("size", quizViewModel.quizDataList.value?.size.toString())
            val listSize = quizViewModel.quizDataList.value?.size
            if (listSize != null) {
                if (listSize <= 3)
                    quizViewModel.addQuiz()
                else
                    Snackbar.make(
                        binding.root,
                        "You are only allowed to Create Only 4 Questions ",
                        Snackbar.LENGTH_SHORT
                    ).show()
            }
        }
        binding.uiBtCancel.setOnClickListener {
            requestToGetQuestionImageFromFile.launch("image/*")
        }
    }

    private fun addQuestion() {
        if (quizViewModel.quizDataList.value!!.size <= 3) {
            quizViewModel.addQuiz()
        } else {
            Snackbar.make(binding.root, "maximum Four Questions allowed ", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun addNewOption(questionPosition: Int) {
        val optionSize = quizViewModel.quizDataList.value?.get(questionPosition)?.options?.size
        if (optionSize != null) {
            if (optionSize <= 3)
                quizViewModel.addOption(questionPosition)
            else
                Snackbar.make(
                    binding.root,
                    "don't create more than 4 options",
                    Snackbar.LENGTH_SHORT
                ).show()
        }
    }

    private fun removeOption(questionPosition: Int, optionPosition: Int) {
        quizViewModel.removeOption(questionPosition, optionPosition)
    }

    private fun removeQuestion(questionPosition: Int) {
        quizViewModel.removeQuestion(questionPosition)
    }

    private fun onQuestionTitleChange(questionTile: String, questionPosition: Int) {
        quizViewModel.onQuestionTitleChange(questionTile, questionPosition)
    }

    private fun onOptionTextChange(optionText: String, questionPosition: Int, optionPosition: Int) {
        quizViewModel.onOptionTextChange(optionText, questionPosition, optionPosition)
    }

    private fun copyQuestion(questionCardPosition: Int, questions: Questions) {
        val listSize = quizViewModel.quizDataList.value?.size
        if (listSize != null) {
            if (listSize <= 3)
                quizViewModel.copyQuestion(questionCardPosition, questions)
            else
                Snackbar.make(
                    binding.root,
                    "You are only allowed to Create Only 4 Questions ",
                    Snackbar.LENGTH_SHORT
                ).show()
        }
    }

    private fun onOptionSelected(questionPosition: Int, optionPosition: String) {
        quizViewModel.onOptionSelected(questionPosition, optionPosition)
    }

    @SuppressLint("InflateParams")
    private fun setAnswerKey(questionPosition: Int) {
        val question = quizViewModel.quizDataList.value

        val questionName = question?.get(questionPosition)?.questionTitle
        if (questionName?.isNotEmpty() == true) {
            val view = layoutInflater.inflate(R.layout.options_select_bottomsheet, null)
            val optionsRv = view.findViewById<RecyclerView>(R.id.uiRvBtmOptionSelect)
            val dialog = BottomSheetDialog(this)
            val btnClose = view.findViewById<ImageView>(R.id.uiIvBottomCloseClose)
            val questionTitle = view.findViewById<TextView>(R.id.uiTvBtmQuestionName)

            if (question[questionPosition].options.size >= 3) {
                var isOptionEmpty = false
                question[questionPosition].options.forEach { options ->
                    if (options.option?.isEmpty() == true)
                        isOptionEmpty = true
                }
                if (isOptionEmpty) {
                    Snackbar.make(binding.root, "Please Enter Options..", Snackbar.LENGTH_SHORT).show()
                } else {
                    setAnswerKeyAdapter = setAnswerKeyAdapter(
                        optionList = question[questionPosition].options,
                        onAnswerKeySelected = { option ->
                            onOptionSelectedFromBottomList(questionPosition, option)
                            dialog.dismiss()
                        }
                    )
                    optionsRv.adapter = setAnswerKeyAdapter
                    questionTitle.text = ("${questionPosition + 1}, " + question[questionPosition].questionTitle)
                    btnClose.setOnClickListener {
                        dialog.dismiss()
                    }
                    dialog.setCancelable(false)
                    dialog.setContentView(view)
                    dialog.show()
                }

            } else {
                Snackbar.make(binding.root, "Minimum 3 Option Required..", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(binding.root, "Question Name Required", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onOptionSelectedFromBottomList(questionPosition: Int, optionId: String) {
        quizViewModel.onOptionSelected(questionPosition, optionId)
    }

}