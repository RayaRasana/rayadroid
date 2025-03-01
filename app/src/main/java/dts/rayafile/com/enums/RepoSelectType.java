package dts.rayafile.com.enums;

/**
 * <pre>
 * NOT_SELECTABLE: no select
 * ONLY_ACCOUNT: only select account
 * ONLY_REPO: only select repo
 * </pre>
 */
public enum RepoSelectType {
    NOT_SELECTABLE(-1),
    ONLY_ACCOUNT(0),
    ONLY_REPO(1),
    DIR(2);

    RepoSelectType(int i) {

    }
}
