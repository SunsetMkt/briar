package org.briarproject.briar.android.conversation;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.briar.android.attachment.AttachmentItem;
import org.briarproject.briar.android.conversation.glide.GlideApp;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import static android.os.Build.VERSION.SDK_INT;
import static android.widget.ImageView.ScaleType.FIT_START;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.NONE;
import static org.briarproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.briarproject.briar.android.conversation.ImageActivity.ATTACHMENT_POSITION;
import static org.briarproject.briar.android.conversation.ImageActivity.ITEM_ID;

@MethodsNotNullByDefault
@ParametersAreNonnullByDefault
public class ImageFragment extends Fragment {

	private final static String IS_FIRST = "isFirst";

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private AttachmentItem attachment;
	private boolean isFirst;
	private MessageId conversationItemId;
	private ImageViewModel viewModel;
	private PhotoView photoView;

	static ImageFragment newInstance(AttachmentItem a,
			MessageId conversationMessageId, boolean isFirst) {
		ImageFragment f = new ImageFragment();
		Bundle args = new Bundle();
		args.putParcelable(ATTACHMENT_POSITION, a);
		args.putBoolean(IS_FIRST, isFirst);
		args.putByteArray(ITEM_ID, conversationMessageId.getBytes());
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Context ctx) {
		super.onAttach(ctx);
		((BaseActivity) requireActivity()).getActivityComponent().inject(this);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = requireNonNull(getArguments());
		attachment = requireNonNull(args.getParcelable(ATTACHMENT_POSITION));
		isFirst = args.getBoolean(IS_FIRST);
		conversationItemId =
				new MessageId(requireNonNull(args.getByteArray(ITEM_ID)));
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_image, container,
				false);

		viewModel = ViewModelProviders.of(requireNonNull(getActivity()),
				viewModelFactory).get(ImageViewModel.class);

		photoView = v.findViewById(R.id.photoView);
		photoView.setScaleLevels(1, 2, 4);
		photoView.setOnClickListener(view -> viewModel.clickImage());

		// Request Listener
		RequestListener<Drawable> listener = new RequestListener<Drawable>() {

			@Override
			public boolean onLoadFailed(@Nullable GlideException e,
					Object model, Target<Drawable> target,
					boolean isFirstResource) {
				if (getActivity() != null && isFirst)
					getActivity().supportStartPostponedEnterTransition();
				return false;
			}

			@Override
			public boolean onResourceReady(Drawable resource, Object model,
					Target<Drawable> target, DataSource dataSource,
					boolean isFirstResource) {
				if (SDK_INT >= 21 && !(resource instanceof Animatable)) {
					// set transition name only when not animatable,
					// because the animation won't start otherwise
					photoView.setTransitionName(
							attachment.getTransitionName(conversationItemId));
				}
				// Move image to the top if overlapping toolbar
				if (viewModel.isOverlappingToolbar(photoView, resource)) {
					photoView.setScaleType(FIT_START);
				}
				if (getActivity() != null && isFirst) {
					getActivity().supportStartPostponedEnterTransition();
				}
				return false;
			}
		};

		// Load Image
		GlideApp.with(this)
				.load(attachment)
				// TODO allow if size < maxTextureSize ?
//				.override(SIZE_ORIGINAL)
				.diskCacheStrategy(NONE)
				.error(R.drawable.ic_image_broken)
				.addListener(listener)
				.into(photoView);

		return v;
	}

}
