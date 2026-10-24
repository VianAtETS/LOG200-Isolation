
// Définition des cases du jeu. 

enum Square{
    EMPTY(0),
    BLOCKED(1),
    BLACK(3),
    RED(5);
    
    private final int value;
    
    Square(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static Square fromValue(int value) {
        for (Square square : Square.values()) {
            if (square.value == value) {
                return square;
            }
        }
        return null;
    }
}
