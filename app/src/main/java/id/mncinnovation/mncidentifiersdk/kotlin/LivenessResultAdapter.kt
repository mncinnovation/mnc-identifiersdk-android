package id.mncinnovation.mncidentifiersdk.kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import id.mncinnovation.face_detection.model.LivenessResult
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.mncidentifiersdk.databinding.ItemLiveDetectionResultBinding

class LivenessResultAdapter(val items: List<LivenessResult.DetectionResult>): RecyclerView.Adapter<LivenessResultAdapter.DetectionResultViewHolder>(){
    inner class DetectionResultViewHolder(val binding: ItemLiveDetectionResultBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectionResultViewHolder {
        return DetectionResultViewHolder(
            ItemLiveDetectionResultBinding
                .inflate(LayoutInflater.from(parent.context),
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: DetectionResultViewHolder, position: Int) {
        with(holder.binding){
            val item = items[position]
            tvTitle.text = item.detectionMode.name
            tvTime.text = item.timeMilis?.toString()
            item.image?.let {
                BitmapUtils.getBitmapFromContentUri(holder.itemView.context.contentResolver, it)
            }?.let {
                ivResult.setImageBitmap(it)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}