/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Fragment support for Expandable List, like {@link android.support.v4.app.ListFragment}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running on
 * Android 3.0 or above, this implementation is still used.
 * 
 * @author strider
 */
public class ExpandableListFragment extends Fragment
        implements OnCreateContextMenuListener, ExpandableListView.OnChildClickListener,
        ExpandableListView.OnGroupExpandListener, ExpandableListView.OnGroupCollapseListener {
    
    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;
    
    final private Handler mHandler = new Handler();
    
    final private Runnable mRequestFocus = new Runnable() {
            
            public void run() {
                mList.focusableViewAvailable(mList);
            }
            
        };
    
    /*final private AdapterView.OnItemClickListener mOnClickListener
                    = new AdapterView.OnItemClickListener() {
            
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick((ExpandableListView) parent, v, position, id);
            }
            
        };*/
    
    ExpandableListAdapter mAdapter;
    ExpandableListView mList;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mListContainer;
    CharSequence mEmptyText;
    boolean mListShown;
    
    public ExpandableListFragment() {
        //
    }
    
    /**
     * Provide default implementation to return a simple expandable list view.
     * Subclasses can override to replace with their own layout. If doing so,
     * the returned view hierarchy <em>must</em> have an ExpandableListView
     * whose id is {@link android.R.id#list android.R.id.list} and can optionally
     * have a sibling view id {@link android.R.id#empty android.R.id.empty}
     * that is to be shown when the list is empty.
     * <p>
     * If you are overriding this method with your own custom content, consider
     * including the standard layout {@link android.R.layout#list_content}
     * in your layout file, so that you continue to retain all of the standard
     * behavior of ExpandableListFragment. In particular, this is currently
     * the only way to have the built-in indeterminant progress state be shown.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final Context context = (Context) this.getActivity();
        
        FrameLayout root = new FrameLayout(context);
        
        // ------------------------------------------------------------------
        
        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);
        
        ProgressBar progress = new ProgressBar(
                context,
                null,
                android.R.attr.progressBarStyleLarge
            );
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        
        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            ));
        
        // ------------------------------------------------------------------
        
        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);
        
        TextView tv = new TextView(context);
        tv.setId(INTERNAL_EMPTY_ID);
        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            ));
        
        ExpandableListView lv = new ExpandableListView(context);
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        lframe.addView(lv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            ));
        
        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            ));
        
        // ------------------------------------------------------------------
        
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT
            ));
        
        return root;
    }
    
    /**
     * Attach to expandable list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ensureList();
    }
    
    /**
     * Detach from expandable list view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mListShown = false;
        mEmptyView = mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        
        super.onDestroyView();
    }
    
    /**
     * Override this to populate the context menu when an item is long pressed.
     * menuInfo will contain an
     * {@link android.widget.ExpandableListView.ExpandableListContextMenuInfo}
     * whose packedPosition is a packed position that should be used with
     * {@link ExpandableListView#getPackedPositionType(long)} and the other
     * similar methods.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        //
    }
    
    /**
     * This method will be called when an item in the expandable list is selected.
     * Subclasses should override. Subclasses can call
     * getExpandableListView().getItemAtPosition(position) if they need to access
     * the data associated with the selected item.
     * 
     * @param l The ExpandableListView where the click happened.
     * @param v The view that was clicked within the ExpandableListView.
     * @param position The position of the view in the expandable list.
     * @param id The row id of the item that was clicked.
     */
    /*public void onListItemClick(ExpandableListView l, View v, int position, long id) {
        //
    }*/
    
    /**
     * Override this for receiving callbacks when a child has been clicked.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
            int childPosition, long id) {
        return false;
    }
    
    /**
     * Override this for receiving callbacks when a group has been expanded.
     */
    @Override
    public void onGroupExpand(int groupPosition) {
        //
    }
    
    /**
     * Override this for receiving callbacks when a group has been collapsed.
     */
    @Override
    public void onGroupCollapse(int groupPosition) {
        //
    }
    
    /**
     * Get the cursor row ID of the currently selected expandable list item.
     */
    public long getSelectedItemId() {
        ensureList();
        
        return mList.getSelectedItemId();
    }
    
    /**
     * Gets the ID of the currently selected group or child.
     * 
     * @return The ID of the currently selected group or child.
     */
    public long getSelectedId() {
        return mList.getSelectedId();
    }
    
    /**
     * Get the position of the currently selected expandable list item.
     */
    public int getSelectedItemPosition() {
        ensureList();
        
        return mList.getSelectedItemPosition();
    }
    
    /**
     * Gets the position (in packed position representation) of the currently
     * selected group or child. Use {@link ExpandableListView#getPackedPositionType},
     * {@link ExpandableListView#getPackedPositionGroup}, and
     * {@link ExpandableListView#getPackedPositionChild} to unpack the returned
     * packed position.
     * 
     * @return A packed position representation containing the currently
     *         selected group or child's position and type.
     */
    public long getSelectedPosition() {
        return mList.getSelectedPosition();
    }
    
    /**
     * Set the currently selected expandable list item to the specified
     * position with the adapter's data.
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureList();
        
        mList.setSelection(position);
    }
    
    /**
     * Sets the selection to the specified group.
     * 
     * @param groupPosition The position of the group that should be selected.
     */
    public void setSelectedGroup(int groupPosition) {
        mList.setSelectedGroup(groupPosition);
    }
    
    /**
     * Sets the selection to the specified child. If the child is in a collapsed
     * group, the group will only be expanded and child subsequently selected if
     * shouldExpandGroup is set to true, otherwise the method will return false.
     * 
     * @param groupPosition The position of the group that contains the child.
     * @param childPosition The position of the child within the group.
     * @param shouldExpandGroup Whether the child's group should be expanded
     *        if it is collapsed.
     * 
     * @return Whether the selection was successfully set on the child.
     */
    public boolean setSelectedChild(int groupPosition, int childPosition,
            boolean shouldExpandGroup) {
        return mList.setSelectedChild(groupPosition, childPosition, shouldExpandGroup);
    }
    
    /**
     * Get the activity's expandable list view widget.
     */
    public ExpandableListView getExpandableListView() {
        ensureList();
        
        return mList;
    }
    
    /**
     * Get the ExpandableListAdapter associated with this activity's
     * ExpandableListView.
     */
    public ExpandableListAdapter getExpandableListAdapter() {
        return mAdapter;
    }
    
    /**
     * Provide the cursor for the expandable list view.
     */
    public void setListAdapter(ExpandableListAdapter adapter) {
        boolean hadAdapter = (mAdapter != null);
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if ((!mListShown) && (!hadAdapter)) {
                // The expandable list was hidden, and previously didn't have
                // an adapter. It is now time to show it.
                setListShown(true, (this.getView().getWindowToken() != null));
            }
        }
    }
    
    /**
     * The default content for an ExpandableListFragment has a TextView that
     * can be shown when the expandable list is empty. If you would like to
     * have it shown, call this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text) {
        ensureList();
        
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view.");
        }
        mStandardEmptyView.setText(text);
        if (mEmptyText == null) {
            mList.setEmptyView(mStandardEmptyView);
        }
        mEmptyText = text;
    }
    
    /**
     * Control whether the expandable list is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminant progress indicator will be shown instead.
     * <p>
     * Applications do not normally need to use this themselves. The default behavior
     * of ExpandableListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with {@link #setListAdapter(ListAdapter)}.
     * If the list at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     * 
     * @param shown If true, the expandable list view is shown; if false,
     *        the progress indicator. The initial value is true.
     */
    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }
    
    /**
     * Like {@link #setListShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }
    
    /**
     * Control whether the expandable list is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminant progress indicator will be shown instead.
     * 
     * @param shown If true, the expandable list view is shown; if false,
     *        the progress indicator. The initial value is true.
     * @param animate If true, an animation will be used to transition to
     *        the new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view.");
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        (Context) this.getActivity(),
                        android.R.anim.fade_out
                    ));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        (Context) this.getActivity(),
                        android.R.anim.fade_in
                    ));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        (Context) this.getActivity(),
                        android.R.anim.fade_in
                    ));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        (Context) this.getActivity(),
                        android.R.anim.fade_out
                    ));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }
    
    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = this.getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created.");
        }
        if (root instanceof ExpandableListView) {
            mList = (ExpandableListView) root;
        } else {
            mStandardEmptyView = (TextView) root.findViewById(INTERNAL_EMPTY_ID);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
            mListContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);
            View rawList = root.findViewById(android.R.id.list);
            if (!(rawList instanceof ExpandableListView)) {
                if (rawList == null) {
                    throw new RuntimeException(
                            "Your content must have an ExpandableListView"
                            + " whose id attribute is 'android.R.id.list'."
                        );
                }
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list'"
                        + " that is not a ExpandableListView class."
                    );
            }
            mList = (ExpandableListView) rawList;
            if (mEmptyView != null) {
                mList.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mList.setEmptyView(mStandardEmptyView);
            }
        }
        mListShown = true;
        //mList.setOnItemClickListener(mOnClickListener);
        mList.setOnChildClickListener(this);
        mList.setOnGroupExpandListener(this);
        mList.setOnGroupCollapseListener(this);
        if (mAdapter != null) {
            ExpandableListAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't have
            // our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }
    
}
