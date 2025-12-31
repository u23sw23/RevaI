package com.example.demo;

public class Treenode {
    int val;
    Treenode left;
    Treenode right;
    public Treenode(){}
    public Treenode(int val){
        this.val=val;
    }
    public Treenode(int val,Treenode left,Treenode right){
        this.left=left;
        this.right=right;
        this.val=val;
    }
}
