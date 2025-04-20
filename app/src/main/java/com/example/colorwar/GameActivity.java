package com.example.colorwar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GameActivity extends AppCompatActivity implements CellAdapter.OnCellClickListener {

    private static final String TAG = "GameActivity";
    private RecyclerView gameGrid;
    private ConstraintLayout mainLayout;
    private CellAdapter cellAdapter;
    private int[] cellStates; // 0: trống, 1-3: đỏ (1-3 chấm), 4-6: xanh (1-3 chấm), 7: đỏ (4 chấm, tạm thời), 8: xanh (4 chấm, tạm thời)
    private boolean isPlayer1Turn = true; // true: Người chơi 1 (đỏ), false: Người chơi 2 (xanh)
    private boolean player1FirstClick = true; // Theo dõi lần nhấn đầu tiên của Người chơi 1
    private boolean player2FirstClick = true; // Theo dõi lần nhấn đầu tiên của Người chơi 2
    private boolean isFirstRound = true; // Theo dõi vòng đầu tiên (người chơi 1 và 2 chưa hoàn thành lượt đầu)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Khởi tạo bảng lưới 5x5
        mainLayout = findViewById(R.id.main_layout);
        gameGrid = findViewById(R.id.game_grid);
        cellStates = new int[25]; // 5x5 = 25 ô
        initializeBoard();

        // Thiết lập RecyclerView
        cellAdapter = new CellAdapter(cellStates, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        gameGrid.setLayoutManager(gridLayoutManager);
        gameGrid.setAdapter(cellAdapter);
        // Thêm khoảng cách giữa các ô
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        gameGrid.addItemDecoration(new GridSpacingItemDecoration(5, spacingInPixels, true));
        // Đặt background ban đầu cho lượt Người chơi 1
        updateBackground();
    }

    private void initializeBoard() {
        // Khởi tạo bảng ban đầu: tất cả ô trống
        for (int i = 0; i < cellStates.length; i++) {
            cellStates[i] = 0; // Trống
        }
    }

    private void updateBackground() {
        // Thay đổi background của ConstraintLayout theo lượt
        mainLayout.setBackgroundResource(isPlayer1Turn ? R.drawable.bg_player1 : R.drawable.bg_player2);
    }

    @Override
    public void onCellClick(int position) {
        int state = cellStates[position];
        Log.d(TAG, "Player " + (isPlayer1Turn ? "1" : "2") + " clicked position: " + position + ", state: " + state);

        // Kiểm tra nếu ô trống và là lần nhấn đầu tiên
        if (state == 0 && (isPlayer1Turn ? player1FirstClick : player2FirstClick)) {
            cellStates[position] = isPlayer1Turn ? 3 : 6; // Đặt 3 chấm (đỏ hoặc xanh)
            if (isPlayer1Turn) {
                player1FirstClick = false;
            } else {
                player2FirstClick = false;
                isFirstRound = false; // Vòng đầu tiên kết thúc
            }
            Log.d(TAG, "Player " + (isPlayer1Turn ? "1" : "2") + " first click: Placed 3 dots at position: " + position);
            applyAnimation(position);
            checkExplosion(position);

            isPlayer1Turn = !isPlayer1Turn; // Chuyển lượt
            updateBackground();
            if (checkGameOver()) return;
            return;
        }

        // Kiểm tra nếu ô thuộc về người chơi hiện tại
        boolean validClick = false;
        if (isPlayer1Turn && state >= 1 && state <= 3) { // Người chơi 1 (đỏ)
            if (state < 3) {
                cellStates[position] = state + 1; // Tăng số chấm
                Log.d(TAG, "Increased red dots at position: " + position + ", new state: " + cellStates[position]);
            } else {
                cellStates[position] = 7; // Tạm thời đặt thành 4 chấm đỏ để kích hoạt nổ
                Log.d(TAG, "Red cell at position: " + position + " reached 4 dots (temp state: 7)");
            }
            validClick = true;
        } else if (!isPlayer1Turn && state >= 4 && state <= 6) { // Người chơi 2 (xanh)
            if (state < 6) {
                cellStates[position] = state + 1; // Tăng số chấm
                Log.d(TAG, "Increased blue dots at position: " + position + ", new state: " + cellStates[position]);
            } else {
                cellStates[position] = 8; // Tạm thời đặt thành 4 chấm xanh để kích hoạt nổ
                Log.d(TAG, "Blue cell at position: " + position + " reached 4 dots (temp state: 8)");
            }
            validClick = true;
        } else {
            Log.d(TAG, "Invalid click by Player " + (isPlayer1Turn ? "1" : "2") + " at position: " + position + ", state: " + state);
        }

        // Nếu nhấn hợp lệ, áp dụng animation và kiểm tra nổ
        if (validClick) {
            applyAnimation(position);
            checkExplosion(position);

            // Kiểm tra điều kiện thắng (chỉ từ lượt thứ hai trở đi)
            if (checkGameOver()) return;

            isPlayer1Turn = !isPlayer1Turn; // Chuyển lượt
            updateBackground();
        }
    }

    private boolean checkGameOver() {
        // Không kiểm tra chiến thắng trong vòng đầu tiên
        if (isFirstRound) {
            Log.d(TAG, "Skipping game over check in first round");
            return false;
        }

        int player1Cells = 0, player2Cells = 0;

        // Đếm số ô của người chơi 1 (đỏ) và người chơi 2 (xanh)
        for (int state : cellStates) {
            if (state >= 1 && state <= 3) player1Cells++; // Đỏ (1-3 chấm)
            if (state >= 4 && state <= 6) player2Cells++; // Xanh (1-3 chấm)
        }

        Log.d(TAG, "Check game over: player1Cells = " + player1Cells + ", player2Cells = " + player2Cells);

        // Kiểm tra điều kiện thắng: Tất cả ô phải cùng một màu
        if (player1Cells == 0 && player2Cells > 0) {
            // Tất cả ô có màu là xanh (không còn ô đỏ)
            showGameOverDialog("Người chơi 2 thắng!");
            return true;
        } else if (player2Cells == 0 && player1Cells > 0) {
            // Tất cả ô có màu là đỏ (không còn ô xanh)
            showGameOverDialog("Người chơi 1 thắng!");
            return true;
        }

        // Nếu cả hai màu vẫn tồn tại, trò chơi chưa kết thúc
        return false;
    }

    private void showGameOverDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Trò chơi kết thúc")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Quay lại màn hình chính hoặc chơi lại
                    Intent intent = new Intent(GameActivity.this, MenuUI.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void checkExplosion(int position) {
        int state = cellStates[position];
        Log.d(TAG, "Checking explosion at position: " + position + ", state: " + state);

        if (state == 7 || state == 8) {
            int baseColor = (state == 7) ? 1 : 4; // 1 cho đỏ, 4 cho xanh
            boolean isRedExplosion = (state == 7); // Nổ đỏ hay xanh
            cellStates[position] = 0;
            Log.d(TAG, "Explosion at position: " + position + ", set to empty");
            applyAnimation(position);

            int row = position / 5;
            int col = position % 5;
            int[] adjacentPositions = new int[4];
            boolean[] validAdjacent = new boolean[4];

            adjacentPositions[0] = (row - 1) * 5 + col; // Lên
            adjacentPositions[1] = (row + 1) * 5 + col; // Xuống
            adjacentPositions[2] = row * 5 + (col - 1); // Trái
            adjacentPositions[3] = row * 5 + (col + 1); // Phải

            validAdjacent[0] = (row - 1) >= 0;
            validAdjacent[1] = (row + 1) < 5;
            validAdjacent[2] = (col - 1) >= 0;
            validAdjacent[3] = (col + 1) < 5;

            for (int i = 0; i < 4; i++) {
                if (validAdjacent[i]) {
                    int adjPos = adjacentPositions[i];
                    int adjState = cellStates[adjPos];
                    Log.d(TAG, "Checking adjacent position: " + adjPos + ", state: " + adjState + ", isRedExplosion: " + isRedExplosion);

                    // Tất cả ô lân cận (bao gồm ô của đối thủ) đều bị ảnh hưởng
                    if (adjState == 0) {
                        // Ô trống: Đặt thành màu của bên gây nổ với 1 chấm
                        cellStates[adjPos] = baseColor;
                        Log.d(TAG, "Set adjacent position: " + adjPos + " to base color: " + baseColor);
                    } else if (adjState >= 1 && adjState <= 6) {
                        // Ô có chấm (đỏ hoặc xanh): Tăng số chấm và đổi màu
                        int dots = (adjState <= 3) ? adjState : (adjState - 3); // Số chấm hiện tại
                        dots++; // Tăng 1 chấm
                        if (dots >= 4) {
                            // Nếu đạt 4 chấm, ô sẽ nổ
                            cellStates[adjPos] = (baseColor == 1) ? 7 : 8;
                            Log.d(TAG, "Adjacent position: " + adjPos + " reached 4 dots, will explode, new state: " + cellStates[adjPos]);
                        } else {
                            // Nếu chưa đạt 4 chấm, đổi màu và cập nhật số chấm
                            cellStates[adjPos] = (baseColor == 1) ? dots : (dots + 3);
                            Log.d(TAG, "Adjacent position: " + adjPos + " changed color and increased dots, new state: " + cellStates[adjPos]);
                        }
                    }

                    applyAnimation(adjPos);
                    checkExplosion(adjPos);
                }
            }
        }
    }

    private void applyAnimation(int position) {
        // Cập nhật ô với animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        View view = gameGrid.findViewHolderForAdapterPosition(position).itemView;
        if (view != null) {
            view.startAnimation(fadeIn);
        } else {
            Log.w(TAG, "View is null for position: " + position);
        }
        cellAdapter.updateCell(position, cellStates[position]);
        Log.d(TAG, "Applied animation at position: " + position + ", state: " + cellStates[position]);
    }
}